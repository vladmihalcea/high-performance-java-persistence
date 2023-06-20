package com.vladmihalcea.hpjp.hibernate.concurrency.updates;

import com.vladmihalcea.hpjp.util.AbstractTest;
import com.vladmihalcea.hpjp.util.exception.ExceptionUtil;
import com.vladmihalcea.hpjp.util.providers.Database;
import org.hibernate.annotations.DynamicUpdate;
import org.junit.Test;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 * @author Vlad Mihalcea
 */
public class PostgreSQLRowLevelLockingTest extends AbstractTest {

    @Override
    protected Class<?>[] entities() {
        return new Class<?>[]{
            Book.class
        };
    }

    @Override
    protected Database database() {
        return Database.POSTGRESQL;
    }

    @Override
    protected void afterInit() {
        doInJPA(entityManager -> {
            entityManager.persist(
                new Book()
                    .setId(1L)
                    .setTitle("High-Performance Java Persistence")
                    .setPriceCents(4495)
            );
        });
    }

    @Test
    public void test() {
        doInJPA(entityManager -> {
            Book post = entityManager.find(Book.class, 1L);
            post.setPriceCents(3995);

            entityManager.flush();

            CountDownLatch bobThreadStartLatch = new CountDownLatch(1);
            CountDownLatch aliceThreadSleepStartLatch = new CountDownLatch(1);

            executeAsync(() -> {
                doInJPA(_entityManager -> {
                    awaitOnLatch(bobThreadStartLatch);

                    LOGGER.info("Bob updates the book record");
                    Book _post = _entityManager.find(Book.class, 1L);
                    _post.setTitle("High-Performance Java Persistence, 2nd edition");

                    aliceThreadSleepStartLatch.countDown();
                    _entityManager.flush();
                });
            });

            bobThreadStartLatch.countDown();
            awaitOnLatch(aliceThreadSleepStartLatch);
            LOGGER.info("Alice's thread sleeps for 1 second");
            sleep(TimeUnit.SECONDS.toMillis(1));
        });

        doInJPA(entityManager -> {
            Book post = entityManager.find(Book.class, 1L);

            assertEquals("High-Performance Java Persistence, 2nd edition", post.getTitle());
            assertEquals(3995, post.getPriceCents());
        });
    }

    @Test
    public void testTimeout() {
        doInJPA(entityManager -> {
            Book post = entityManager.find(Book.class, 1L);
            post.setPriceCents(3995);

            LOGGER.info("Alice updates the book record");
            entityManager.flush();

            executeSync(() -> {
                doInJPA(_entityManager -> {
                    executeStatement(_entityManager, "SET lock_timeout TO '1s'");

                    Book _post = _entityManager.find(Book.class, 1L);
                    _post.setTitle("High-Performance Java Persistence, 2nd edition");

                    LOGGER.info("Bob updates the book record");
                    try {
                        _entityManager.flush();
                    } catch (Exception expected) {
                        assertTrue(
                            ExceptionUtil.rootCause(expected)
                                .getMessage()
                                .contains("canceling statement due to lock timeout")
                        );
                        LOGGER.error("Lock acquisition failure: ", expected);
                    }
                });
            });
        });
    }

    @Entity(name = "Book")
    @Table(name = "book")
    @DynamicUpdate
    public static class Book {

        @Id
        private Long id;

        private String title;

        @Column(name = "price_cents")
        private int priceCents;

        public Long getId() {
            return id;
        }

        public Book setId(Long id) {
            this.id = id;
            return this;
        }

        public String getTitle() {
            return title;
        }

        public Book setTitle(String title) {
            this.title = title;
            return this;
        }

        public int getPriceCents() {
            return priceCents;
        }

        public Book setPriceCents(int priceCents) {
            this.priceCents = priceCents;
            return this;
        }
    }
}
