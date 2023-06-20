package com.vladmihalcea.hpjp.hibernate.concurrency;

import com.vladmihalcea.hpjp.util.AbstractTest;
import com.vladmihalcea.hpjp.util.exception.ExceptionUtil;
import com.vladmihalcea.hpjp.util.providers.Database;
import jakarta.persistence.*;
import org.hibernate.LockOptions;
import org.hibernate.jpa.SpecHints;
import org.junit.Test;

import java.util.Map;

import static org.junit.Assert.*;

/**
 * @author Vlad Mihalcea
 */
public class NoWaitTest extends AbstractTest {

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

    public void afterInit() {
        doInJPA(entityManager -> {
            for (long i = 1; i <= 10; i++) {
                Post post = new Post();
                post.setId(i);
                post.setTitle("High-Performance Java Persistence");
                entityManager.persist(post);
            }
        });
    }

    @Test
    public void testLockContention() {
        LOGGER.info("Test lock contention");

        doInJPA(entityManager -> {
            assertNotNull(
                getAndLockPost(
                    entityManager,
                    1L
                )
            );

            try {
                executeSync(() -> {
                    doInJPA(_entityManager -> {
                        assertNotNull(
                            getAndLockPost(
                                _entityManager,
                                1L
                            )
                        );
                    });
                });
            } catch (Exception e) {
                assertTrue(ExceptionUtil.isLockTimeout(e));
            }
        });
    }

    public Post getAndLockPost(EntityManager entityManager, Long postId) {
        return entityManager.find(
            Post.class,
            postId,
            LockModeType.PESSIMISTIC_WRITE,
            Map.of(SpecHints.HINT_SPEC_LOCK_TIMEOUT, LockOptions.NO_WAIT)
        );
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
}
