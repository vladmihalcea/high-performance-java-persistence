package com.vladmihalcea.book.hpjp.hibernate.concurrency.deadlock.fk;

import com.vladmihalcea.book.hpjp.util.AbstractTest;
import com.vladmihalcea.book.hpjp.util.providers.Database;
import org.hibernate.Session;
import org.junit.Test;

import jakarta.persistence.*;
import java.sql.Connection;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Future;

/**
 * @author Vlad Mihalcea
 */
public class OracleNoFKNoParentLockSerializableTest extends AbstractTest {

    private final int ISOLATION_LEVEL = Connection.TRANSACTION_SERIALIZABLE;

    @Override
    protected Class<?>[] entities() {
        return new Class<?>[] {
            Post.class,
            PostComment.class
        };
    }

    @Override
    protected Database database() {
        return Database.ORACLE;
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
    }

    @Test
    public void test() {
        CountDownLatch bobStart = new CountDownLatch(1);
        doInJPA(entityManager -> {
            prepareConnection(entityManager);

            LOGGER.info("Alice updates the Post entity");
            Post post = entityManager.find(Post.class, 1L);
            post.setTitle("High-Performance Java Persistence 2nd edition");
            entityManager.flush();

            Future<?> future = executeAsync(() -> {
                doInJPA(_entityManager -> {
                    prepareConnection(_entityManager);

                    LOGGER.info("Bob updates the PostComment entity");
                    PostComment _comment = _entityManager.find(PostComment.class, 1L);
                    _comment.setReview("Great!");
                    bobStart.countDown();
                    _entityManager.flush();
                });
            });

            awaitOnLatch(bobStart);

            try {
                future.get();
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        });
    }

    protected void prepareConnection(EntityManager entityManager) {
        entityManager.unwrap(Session.class).doWork(connection -> {
            connection.setTransactionIsolation(ISOLATION_LEVEL);
            setJdbcTimeout(connection);
        });
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
