package com.vladmihalcea.book.hpjp.hibernate.concurrency.deadlock;

import com.vladmihalcea.book.hpjp.util.AbstractTest;
import com.vladmihalcea.book.hpjp.util.providers.Database;
import org.hibernate.testing.util.ExceptionUtil;
import org.junit.Before;
import org.junit.Test;

import jakarta.persistence.*;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Future;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

/**
 * @author Vlad Mihalcea
 */
public class DeadLockTest extends AbstractTest {

    @Override
    protected Class<?>[] entities() {
        return new Class<?>[] {
            Post.class,
            PostComment.class
        };
    }

    @Override
    public void afterInit() {
        doInJPA(entityManager -> {
            Post post = new Post();
            post.setId(1L);
            post.setTitle("High-Performance Java Persistence");
            entityManager.persist(post);

            PostComment comment = new PostComment();
            comment.setId(1L);
            comment.setReview("Awesome!");
            comment.setPost(post);
            entityManager.persist(comment);
        });
    }

    @Test
    public void testDeadLock() {
        CountDownLatch bobStart = new CountDownLatch(1);
        try {
            doInJPA(entityManager -> {
                LOGGER.info("Alice locks the Post entity");
                Post post = entityManager.find(Post.class, 1L, LockModeType.PESSIMISTIC_WRITE);

                Future<?> future = executeAsync(() -> {
                    doInJPA(_entityManager -> {
                        LOGGER.info("Bob locks the PostComment entity");
                        PostComment _comment = _entityManager.find(PostComment.class, 1L, LockModeType.PESSIMISTIC_WRITE);
                        bobStart.countDown();
                        LOGGER.info("Bob wants to lock the Post entity");
                        Post _post = _entityManager.find(Post.class, 1L, LockModeType.PESSIMISTIC_WRITE);
                    });
                });

                awaitOnLatch(bobStart);
                LOGGER.info("Alice wants to lock the PostComment entity");
                PostComment comment = entityManager.find(PostComment.class, 1L, LockModeType.PESSIMISTIC_WRITE);

                try {
                    future.get();
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
            });
        } catch (Exception e) {
            LOGGER.info("Deadlock detected", ExceptionUtil.rootCause(e));
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
    @Table(name = "post_comment")
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
