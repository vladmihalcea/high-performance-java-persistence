package com.vladmihalcea.book.hpjp.hibernate.concurrency;

import com.fasterxml.jackson.databind.node.ObjectNode;
import com.vladmihalcea.book.hpjp.util.AbstractTest;
import com.vladmihalcea.book.hpjp.util.exception.ExceptionUtil;
import com.vladmihalcea.book.hpjp.util.providers.Database;
import com.vladmihalcea.hibernate.type.json.internal.JacksonUtil;
import org.hibernate.StaleStateException;
import org.junit.Test;

import jakarta.persistence.*;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 * @author Vlad Mihalcea
 */
public class DefaultOptimisticLockingTest extends AbstractTest {

    @Override
    protected Class<?>[] entities() {
        return new Class<?>[]{
                Post.class
        };
    }

    @Override
    protected Database database() {
        return Database.POSTGRESQL;
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
    public void testOptimisticLockExceptionUpdate() {

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

    @Test
    public void testOptimisticLockExceptionRemove() {

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

                        _entityManager.remove(_post);
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

    @Test
    public void testOptimisticLockExceptionMerge() {

        Post _post = doInJPA(entityManager -> {
            Post post = new Post();
            post.setId(1L);
            post.setTitle("High-Performance Java Persistence");

            entityManager.persist(post);

            return post;
        });

        doInJPA(_entityManager -> {
            Post post = _entityManager.find(Post.class, 1L);
            post.setTitle("High-Performance JDBC");
        });

        _post.setTitle("High-Performance Hibernate");

        try {
            doInJPA(entityManager -> {
                entityManager.merge(_post);
            });
        } catch (Exception expected) {
            LOGGER.error("Throws", expected);
            assertEquals(OptimisticLockException.class, expected.getClass());
            assertTrue(StaleStateException.class.isAssignableFrom(ExceptionUtil.rootCause(expected).getClass()));
        }
    }

    @Test
    public void testOptimisticLockExceptionMergeJsonTransform() {

        String postJsonString = doInJPA(entityManager -> {
            Post post = new Post();
            post.setId(1L);
            post.setTitle("High-Performance Java Persistence");

            entityManager.persist(post);

            return JacksonUtil.toString(post);
        });

        doInJPA(_entityManager -> {
            Post post = _entityManager.find(Post.class, 1L);
            post.setTitle("High-Performance JDBC");
        });

        ObjectNode postJsonNode = (ObjectNode) JacksonUtil.toJsonNode(postJsonString);
        postJsonNode.put("title", "High-Performance Hibernate");

        try {
            doInJPA(entityManager -> {
                Post detachedPost = JacksonUtil.fromString(postJsonNode.toString(), Post.class);
                entityManager.merge(detachedPost);
            });
        } catch (Exception expected) {
            LOGGER.error("Throws", expected);
            assertEquals(OptimisticLockException.class, expected.getClass());
            assertTrue(ExceptionUtil.rootCause(expected) instanceof StaleStateException);
        }
    }

    @Entity(name = "Post")  @Table(name = "post")
    public static class Post {

        @Id
        private Long id;

        private String title;

        @Version
        private int version;

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
