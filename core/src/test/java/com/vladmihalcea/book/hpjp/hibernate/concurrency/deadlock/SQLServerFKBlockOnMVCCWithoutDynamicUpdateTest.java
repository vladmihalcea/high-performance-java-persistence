package com.vladmihalcea.book.hpjp.hibernate.concurrency.deadlock;

import com.vladmihalcea.book.hpjp.util.AbstractTest;
import com.vladmihalcea.book.hpjp.util.providers.Database;
import com.vladmihalcea.hibernate.type.util.ListResultTransformer;
import com.vladmihalcea.hibernate.type.util.StringUtils;
import org.hibernate.Session;
import org.hibernate.annotations.DynamicUpdate;
import org.hibernate.testing.util.ExceptionUtil;
import org.junit.Test;

import javax.persistence.*;
import java.util.Date;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Future;
import java.util.stream.Collectors;

/**
 * @author Vlad Mihalcea
 */
public class SQLServerFKBlockOnMVCCWithoutDynamicUpdateTest extends AbstractTest {

    @Override
    protected Class<?>[] entities() {
        return new Class<?>[] {
            Post.class,
            PostComment.class
        };
    }

    @Override
    protected Database database() {
        return Database.SQLSERVER;
    }

    @Override
    public void afterInit() {
        doInJPA(entityManager -> {
            long postId = 1;
            long commentId = 1;
            for (long i = 0; i < 1000; i++) {
                Post post = new Post();
                post.setId(postId++);
                post.setTitle("High-Performance Java Persistence");
                entityManager.persist(post);

                for (long j = 0; j < 10; j++) {
                    PostComment comment = new PostComment();
                    comment.setId(commentId++);
                    comment.setReview("Awesome!");
                    comment.setPost(post);

                    entityManager.persist(comment);
                }

                if(i > 0 && i % 100 == 0) {
                    entityManager.flush();
                }
            }
        });
        ddl("ALTER DATABASE [high_performance_java_persistence] SET READ_COMMITTED_SNAPSHOT ON");
    }

    /**
     * For SQL Server, enable the following trace options:
     *
     * DBCC TRACEON (1204, 1222, -1)
     *
     * And, read the error log using the following SP:
     *
     * sp_readerrorlog
     */
    @Test
    public void testDeadLock() {
        List<ErrorLogMessage> beforeDeadLockErrorLogLines = errorLogMessages();
        ddl("DBCC TRACEON (1204, 1222, -1)");

        CountDownLatch bobStart = new CountDownLatch(1);
        try {
            doInJPA(entityManager -> {
                LOGGER.info("Alice locks the Post entity");
                Post post = entityManager.find(Post.class, 1L);
                post.setTitle("High-Performance Java Persistence 2nd edition");
                entityManager.flush();

                Future<?> future = executeAsync(() -> {
                    doInJPA(_entityManager -> {
                        LOGGER.info("Bob locks the PostComment entity");
                        PostComment _comment = _entityManager.find(PostComment.class, 1L);
                        _comment.setReview("Great!");
                        _entityManager.flush();
                        bobStart.countDown();
                        LOGGER.info("Bob wants to lock the Post entity");
                        Post _post = _entityManager.find(Post.class, 1L);
                        _post.setTitle("High-Performance Java Persistence 3rd edition");
                        _entityManager.flush();
                    });
                });

                awaitOnLatch(bobStart);
                LOGGER.info("Alice wants to lock the PostComment entity");
                PostComment comment = entityManager.find(PostComment.class, 1L);
                comment.setReview("Amazing!");
                entityManager.flush();

                try {
                    future.get();
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
            });
        } catch (Exception e) {
            LOGGER.info("Deadlock detected", ExceptionUtil.rootCause(e));
            List<ErrorLogMessage> afterDeadLockErrorLogLines = errorLogMessages();
            afterDeadLockErrorLogLines.removeAll(beforeDeadLockErrorLogLines);

            LOGGER.info(
                "Deadlock trace info: {}",
                afterDeadLockErrorLogLines.stream().map(ErrorLogMessage::getMessage).collect(Collectors.joining(StringUtils.LINE_SEPARATOR))
            );
        } finally {
            ddl("DBCC TRACEOFF (1204, 1222, -1)");
        }
    }

    private List<ErrorLogMessage> errorLogMessages() {
        try(Session session = entityManagerFactory().createEntityManager().unwrap(Session.class)) {
            return session
                .createNativeQuery("sp_readerrorlog")
                .setResultTransformer((ListResultTransformer) (tuple, aliases) -> new ErrorLogMessage((Date) tuple[0], (String) tuple[2]))
                .getResultList();
        }
    }

    private static class ErrorLogMessage {
        private final Date timestamp;
        private final String message;

        public ErrorLogMessage(Date timestamp, String message) {
            this.timestamp = timestamp;
            this.message = message;
        }

        public Date getTimestamp() {
            return timestamp;
        }

        public String getMessage() {
            return message;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (!(o instanceof ErrorLogMessage)) return false;
            ErrorLogMessage that = (ErrorLogMessage) o;
            return Objects.equals(timestamp, that.timestamp) && Objects.equals(message, that.message);
        }

        @Override
        public int hashCode() {
            return Objects.hash(timestamp, message);
        }
    }

    @Entity(name = "Post")
    @Table(name = "post")
    public static class Post {

        @Id
        private Long id;

        private String title;

        public Long getId() {
            return id;
        }

        public void setId(Long id) {
            this.id = id;
        }

        public String getTitle() {
            return title;
        }

        public void setTitle(String title) {
            this.title = title;
        }
    }

    @Entity(name = "PostComment")
    @Table(
        name = "post_comment",
        indexes = @Index(
            name = "FK_post_comment_post_id",
            columnList = "post_id"
        )
    )
    public static class PostComment {

        @Id
        private Long id;

        @ManyToOne(fetch = FetchType.LAZY)
        private Post post;

        private String review;

        public Long getId() {
            return id;
        }

        public void setId(Long id) {
            this.id = id;
        }

        public Post getPost() {
            return post;
        }

        public void setPost(Post post) {
            this.post = post;
        }

        public String getReview() {
            return review;
        }

        public void setReview(String review) {
            this.review = review;
        }
    }
}
