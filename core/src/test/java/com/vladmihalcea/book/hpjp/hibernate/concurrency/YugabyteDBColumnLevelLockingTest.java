package com.vladmihalcea.book.hpjp.hibernate.concurrency;

import com.vladmihalcea.book.hpjp.util.AbstractTest;
import com.vladmihalcea.book.hpjp.util.providers.Database;
import org.hibernate.StaleStateException;
import org.hibernate.annotations.DynamicUpdate;
import org.hibernate.annotations.OptimisticLockType;
import org.hibernate.annotations.OptimisticLocking;
import org.junit.Test;

import javax.persistence.*;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 * @author Vlad Mihalcea
 */
public class YugabyteDBColumnLevelLockingTest extends AbstractTest {

    @Override
    protected Class<?>[] entities() {
        return new Class<?>[]{
            Post.class
        };
    }

    @Override
    protected Database database() {
        return Database.YUGABYTEDB;
    }

    @Override
    protected void afterInit() {
        doInJPA(entityManager -> {
            entityManager.persist(
                new Post()
                    .setId(1L)
                    .setTitle("High-Performance Java Persistence")
                    .setPriceCents(2495)
            );
        });
    }

    @Test
    public void test() {
        try {
            doInJPA(entityManager -> {
                Post post = entityManager.find(Post.class, 1L);
                post.setPriceCents(1995);

                entityManager.flush();

                executeSync(() -> {
                    doInJPA(_entityManager -> {
                        Post _post = _entityManager.find(Post.class, 1L);
                        _post.setTitle("High-Performance Java Persistence, 2nd edition");
                    });
                });
            });
        } catch (Exception expected) {
            LOGGER.error("Throws", expected);
            assertEquals(PersistenceException.class, expected.getCause().getClass());
        }
    }

    @Entity(name = "Post")
    @Table(name = "post")
    @OptimisticLocking(type = OptimisticLockType.DIRTY)
    @DynamicUpdate
    public static class Post {

        @Id
        private Long id;

        private String title;

        private Integer priceCents;

        public Long getId() {
            return id;
        }

        public Post setId(Long id) {
            this.id = id;
            return this;
        }

        public String getTitle() {
            return title;
        }

        public Post setTitle(String title) {
            this.title = title;
            return this;
        }

        public Integer getPriceCents() {
            return priceCents;
        }

        public Post setPriceCents(Integer priceCents) {
            this.priceCents = priceCents;
            return this;
        }
    }
}
