package com.vladmihalcea.hpjp.hibernate.concurrency.updates;

import com.vladmihalcea.hpjp.util.AbstractTest;
import com.vladmihalcea.hpjp.util.exception.ExceptionUtil;
import com.vladmihalcea.hpjp.util.providers.Database;
import org.hibernate.Session;
import org.hibernate.annotations.DynamicUpdate;
import org.junit.Test;

import jakarta.persistence.*;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 * @author Vlad Mihalcea
 */
public class YugabyteDBColumnLevelLockingReadCommittedTest extends AbstractTest {

    @Override
    protected Class<?>[] entities() {
        return new Class<?>[]{
            Book.class
        };
    }

    @Override
    protected Database database() {
        return Database.YUGABYTEDB;
    }

    @Override
    public void init() {
        if(!ENABLE_LONG_RUNNING_TESTS) {
            return;
        }
        super.init();
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
        if(!ENABLE_LONG_RUNNING_TESTS) {
            return;
        }
        doInJPA(entityManager -> {
            Book post = entityManager.find(Book.class, 1L);
            post.setPriceCents(3995);

            entityManager.flush();

            executeSync(() -> {
                doInJPA(_entityManager -> {
                    Book _post = _entityManager.find(Book.class, 1L);
                    _post.setTitle("High-Performance Java Persistence, 2nd edition");
                });
            });
        });

        doInJPA(entityManager -> {
            Book post = entityManager.find(Book.class, 1L);

            assertEquals("High-Performance Java Persistence, 2nd edition", post.getTitle());
            assertEquals(3995, post.getPriceCents());
        });
    }

    @Test
    public void testUpdateSameColumn() {
        if(!ENABLE_LONG_RUNNING_TESTS) {
            return;
        }
        doInJPA(entityManager -> {
            executeStatement(entityManager, "set default_transaction_isolation to \"read committed\";");

            Book post = entityManager.find(Book.class, 1L);
            post.setTitle("High-Performance Java Persistence, 2022 edition");

            LOGGER.info("Alice updates the book record");
            entityManager.flush();

            executeSync(() -> {
                doInJPA(_entityManager -> {
                    executeStatement(_entityManager, "set default_transaction_isolation to \"read committed\";");
                    _entityManager.unwrap(Session.class).doWork(this::setJdbcTimeout);

                    Book _post = _entityManager.find(Book.class, 1L);
                    _post.setTitle("High-Performance Java Persistence, 2nd edition");

                    LOGGER.info("Bob updates the book record");
                    try {
                        _entityManager.flush();
                    } catch (Exception expected) {
                        assertTrue(
                            ExceptionUtil.rootCause(expected)
                                .getMessage()
                                .contains("Read timeout")
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
