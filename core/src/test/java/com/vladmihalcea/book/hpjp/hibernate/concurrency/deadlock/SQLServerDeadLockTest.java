package com.vladmihalcea.book.hpjp.hibernate.concurrency.deadlock;

import com.vladmihalcea.book.hpjp.util.AbstractTest;
import com.vladmihalcea.book.hpjp.util.providers.Database;
import com.vladmihalcea.hibernate.query.ListResultTransformer;
import com.vladmihalcea.hibernate.util.StringUtils;
import org.hibernate.Session;
import org.hibernate.annotations.DynamicUpdate;
import org.hibernate.testing.util.ExceptionUtil;
import org.junit.Test;

import jakarta.persistence.*;
import java.util.Date;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Future;
import java.util.stream.Collectors;

/**
 * @author Vlad Mihalcea
 */
public class SQLServerDeadLockTest extends AbstractTest {

    @Override
    protected Class<?>[] entities() {
        return new Class<?>[] {
            Post.class,
            PostDetails.class
        };
    }

    @Override
    protected Database database() {
        return Database.SQLSERVER;
    }

    @Override
    public void afterInit() {
        doInJPA(entityManager -> {
            Post post = new Post();
            post.setId(1L);
            post.setTitle("High-Performance Java Persistence");
            entityManager.persist(post);

            PostDetails details = new PostDetails();
            details.setId(1L);
            details.setPost(post);
            entityManager.persist(details);
        });
        executeStatement("ALTER DATABASE [high_performance_java_persistence] SET READ_COMMITTED_SNAPSHOT ON");
    }

    @Override
    public void destroy() {
        executeStatement("ALTER DATABASE [high_performance_java_persistence] SET READ_COMMITTED_SNAPSHOT OFF");
        super.destroy();
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
    public void testDeadLock_1204() {
        LOGGER.info("Check flag: 1204");
        List<ErrorLogMessage> beforeDeadLockErrorLogLines = errorLogMessages();

        try {
            CountDownLatch bobStart = new CountDownLatch(1);

            executeStatement("DBCC TRACEON (1204, -1)");

            doInJPA(entityManager -> {
                LOGGER.info("Alice updates the PostDetails entity");
                PostDetails details = entityManager.find(PostDetails.class, 1L);
                details.setUpdatedBy("Alice");
                entityManager.flush();

                Future<?> future = executeAsync(() -> {
                    doInJPA(_entityManager -> {
                        LOGGER.info("Bob updates the Post entity");
                        Post _post = _entityManager.find(Post.class, 1L);
                        _post.setTitle("ACID");
                        _entityManager.flush();

                        bobStart.countDown();
                        LOGGER.info("Bob wants to update the PostDetails entity");
                        PostDetails _details = _entityManager.find(PostDetails.class, 1L);
                        _details.setUpdatedBy("Bob");
                        _entityManager.flush();
                    });
                });

                awaitOnLatch(bobStart);
                LOGGER.info("Alice wants to update the Post entity");
                Post post = entityManager.find(Post.class, 1L);
                post.setTitle("BASE");
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
                "Deadlock trace info for flag 1204: {}",
                afterDeadLockErrorLogLines.stream().map(ErrorLogMessage::getMessage).collect(Collectors.joining(StringUtils.LINE_SEPARATOR))
            );
        } finally {
            executeStatement("DBCC TRACEOFF (1204, -1)");
        }
    }

    @Test
    public void testDeadLock_1222() {

        LOGGER.info("Check flag: 1222");
        List<ErrorLogMessage> beforeDeadLockErrorLogLines = errorLogMessages();

        try {
            CountDownLatch bobStart = new CountDownLatch(1);

            executeStatement("DBCC TRACEON (1222, -1)");

            doInJPA(entityManager -> {
                LOGGER.info("Alice updates the PostDetails entity");
                PostDetails details = entityManager.find(PostDetails.class, 1L);
                details.setUpdatedBy("Alice");
                entityManager.flush();

                Future<?> future = executeAsync(() -> {
                    doInJPA(_entityManager -> {
                        LOGGER.info("Bob updates the Post entity");
                        Post _post = _entityManager.find(Post.class, 1L);
                        _post.setTitle("ACID");
                        _entityManager.flush();

                        bobStart.countDown();
                        LOGGER.info("Bob wants to update the PostDetails entity");
                        PostDetails _details = _entityManager.find(PostDetails.class, 1L);
                        _details.setUpdatedBy("Bob");
                        _entityManager.flush();
                    });
                });

                awaitOnLatch(bobStart);
                LOGGER.info("Alice wants to update the Post entity");
                Post post = entityManager.find(Post.class, 1L);
                post.setTitle("BASE");
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
                "Deadlock trace info for flag 1222: {}",
                afterDeadLockErrorLogLines.stream().map(ErrorLogMessage::getMessage).collect(Collectors.joining(StringUtils.LINE_SEPARATOR))
            );
        } finally {
            executeStatement("DBCC TRACEOFF (1222, -1)");
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
    @DynamicUpdate
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

    @Entity(name = "PostDetails")
    @Table(name = "post_details")
    @DynamicUpdate
    public static class PostDetails {

        @Id
        private Long id;

        @Column(name = "updated_by")
        private String updatedBy;

        @OneToOne(fetch = FetchType.LAZY)
        @MapsId
        private Post post;

        public Long getId() {
            return id;
        }

        public PostDetails setId(Long id) {
            this.id = id;
            return this;
        }

        public Post getPost() {
            return post;
        }

        public PostDetails setPost(Post post) {
            this.post = post;
            return this;
        }

        public String getUpdatedBy() {
            return updatedBy;
        }

        public PostDetails setUpdatedBy(String updatedBy) {
            this.updatedBy = updatedBy;
            return this;
        }
    }
}
