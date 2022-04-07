package com.vladmihalcea.book.hpjp.hibernate.concurrency;

import com.vladmihalcea.book.hpjp.util.AbstractTest;
import org.hibernate.StaleStateException;
import org.junit.Test;

import jakarta.persistence.*;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 * @author Vlad Mihalcea
 */
public class VersionWrapperOptimisticLockingTest extends AbstractTest {

    @Override
    protected Class<?>[] entities() {
        return new Class<?>[]{
            Post.class
        };
    }

    @Test
    public void testOptimisticLocking() {

        doInJPA(entityManager -> {
            Post post = new Post();
            post.setId(1L);
            post.setTitle("High-Performance Java Persistence");
            entityManager.persist(post);

            entityManager.flush();
            post.setTitle("High-Performance Hibernate");
        });

        doInJPA(entityManager -> {
            Post post = entityManager.find(Post.class, 1L);
            assertEquals(1, post.getVersion());
        });
    }

    @Test
    public void testStaleStateException() {

        doInJPA(entityManager -> {
            Post post = new Post();
            post.setId(1L);
            post.setTitle("High-Performance Java Persistence");
            entityManager.persist(post);
        });

        try {
            doInJPA(entityManager -> {
                Post post = entityManager.find(Post.class, 1L);

                executeSync(() -> {
                    doInJPA(_entityManager -> {
                        Post _post = _entityManager.find(Post.class, 1L);
                        _post.setTitle("High-Performance JDBC");
                    });
                });

                post.setTitle("High-Performance Hibernate");
            });
        } catch (Exception expected) {
            LOGGER.error("Throws", expected);
            assertEquals(OptimisticLockException.class, expected.getCause().getClass());
            assertTrue(StaleStateException.class.isAssignableFrom(expected.getCause().getCause().getClass()));
        }
    }

    @Entity(name = "Post")  @Table(name = "post")
    public static class Post {

        @Id
        private Long id;

        private String title;

        @Version
        private Integer version;

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

        public int getVersion() {
            return version;
        }
    }
}
