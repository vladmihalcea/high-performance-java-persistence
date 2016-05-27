package com.vladmihalcea.book.hpjp.hibernate.concurrency;

import com.vladmihalcea.book.hpjp.util.AbstractTest;
import org.hibernate.Session;
import org.hibernate.StaleObjectStateException;
import org.hibernate.annotations.DynamicUpdate;
import org.hibernate.annotations.OptimisticLockType;
import org.hibernate.annotations.OptimisticLocking;
import org.junit.Test;

import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Table;
import java.util.concurrent.CountDownLatch;

/**
 * OptimisticLockingOneRootDirtyVersioningTest - Test to check optimistic checking on a single entity being updated by many threads
 * using the dirty properties instead of a synthetic version column
 *
 * @author Vlad Mihalcea
 */
public class OptimisticLockingOneRootDirtyVersioningTest extends AbstractTest {

    private final CountDownLatch loadPostLatch = new CountDownLatch(3);
    private final CountDownLatch aliceLatch = new CountDownLatch(1);

    @Override
    protected Class<?>[] entities() {
        return new Class<?>[]{
                Post.class
        };
    }

    public class AliceTransaction implements Runnable {

        @Override
        public void run() {
            try {
                doInJPA(entityManager -> {
                    try {
                        Post post = entityManager.find(Post.class, 1L);
                        loadPostLatch.countDown();
                        loadPostLatch.await();
                        post.setTitle("JPA");
                    } catch (InterruptedException e) {
                        throw new IllegalStateException(e);
                    }
                });
            } catch (StaleObjectStateException expected) {
                LOGGER.info("Alice: Optimistic locking failure", expected);
            }
            aliceLatch.countDown();
        }
    }

    public class BobTransaction implements Runnable {

        @Override
        public void run() {
            try {
                doInJPA(entityManager -> {
                    try {
                        Post post = entityManager.find(Post.class, 1L);
                        loadPostLatch.countDown();
                        loadPostLatch.await();
                        aliceLatch.await();
                        post.incrementLikes();
                    } catch (InterruptedException e) {
                        throw new IllegalStateException(e);
                    }
                });
            } catch (StaleObjectStateException expected) {
                LOGGER.info("Bob: Optimistic locking failure", expected);
            }
        }
    }

    public class CarolTransaction implements Runnable {

        @Override
        public void run() {
            try {
                doInJPA(entityManager -> {
                    try {
                        Post post = entityManager.find(Post.class, 1L);
                        loadPostLatch.countDown();
                        loadPostLatch.await();
                        aliceLatch.await();
                        post.setViews(15);
                    } catch (InterruptedException e) {
                        throw new IllegalStateException(e);
                    }
                });
            } catch (StaleObjectStateException expected) {
                LOGGER.info("Carol: Optimistic locking failure", expected);
            }
        }
    }

    @Test
    public void testOptimisticLocking() throws InterruptedException {

        doInJPA(entityManager -> {
            Post post = new Post();
            post.setId(1L);
            post.setTitle("JDBC");
            entityManager.persist(post);
        });

        Thread alice = new Thread(new AliceTransaction());
        Thread bob = new Thread(new BobTransaction());
        Thread carol = new Thread(new CarolTransaction());

        alice.start();
        bob.start();
        carol.start();

        alice.join();
        bob.join();
        carol.join();
    }

    @Test
    public void testVersionlessOptimisticLockingWhenMerging() {
        Post detachedPost = doInJPA(entityManager -> {
            Post post = new Post();
            post.setId(1L);
            post.setTitle("JDBC");
            entityManager.persist(post);
            return post;
        });

        doInJPA(entityManager -> {
            Post post = entityManager.find(Post.class, 1L);
            post.setTitle("Hibernate");
            return post;
        });

        doInJPA(entityManager -> {
            detachedPost.setTitle("JPA");
            entityManager.merge(detachedPost);
        });
    }

    @Test
    public void testVersionlessOptimisticLockingWhenReattaching() {

        doInJPA(entityManager -> {
            Post post = new Post();
            post.setId(1L);
            post.setTitle("JDBC");
            entityManager.persist(post);
            return post;
        });

        Post detachedPost = doInJPA(entityManager -> {
            LOGGER.info("Alice loads the Post entity");
            return entityManager.find(Post.class, 1L);
        });

        executeSync(() -> {
            doInJPA(entityManager -> {
                LOGGER.info("Bob loads the Post entity and modifies it");
                Post post = entityManager.find(Post.class, 1L);
                post.setTitle("Hibernate");
            });
        });

        doInJPA(entityManager -> {
            LOGGER.info("Alice updates the Post entity");
            detachedPost.setTitle("JPA");
            entityManager.unwrap(Session.class).update(detachedPost);
        });
    }
    
    @Entity(name = "Post") @Table(name = "post")
    @OptimisticLocking(type = OptimisticLockType.DIRTY)
    @DynamicUpdate
    public static class Post {

        @Id
        private Long id;

        private String title;

        private long views;

        private int likes;

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

        public long getViews() {
            return views;
        }

        public int getLikes() {
            return likes;
        }

        public int incrementLikes() {
            return ++likes;
        }

        public void setViews(long views) {
            this.views = views;
        }
    }
}
