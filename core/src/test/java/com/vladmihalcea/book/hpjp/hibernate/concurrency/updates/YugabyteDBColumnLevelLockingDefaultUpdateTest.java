package com.vladmihalcea.book.hpjp.hibernate.concurrency.updates;

import com.vladmihalcea.book.hpjp.util.AbstractTest;
import com.vladmihalcea.book.hpjp.util.providers.Database;
import org.junit.Test;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Table;

/**
 * @author Vlad Mihalcea
 */
public class YugabyteDBColumnLevelLockingDefaultUpdateTest extends AbstractTest {

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
        try {
            doInJPA(entityManager -> {
                Book post = entityManager.find(Book.class, 1L);
                post.setPriceCents(3995);

                LOGGER.info("Alice updates the book record");
                entityManager.flush();

                executeSync(() -> {
                    try {
                        doInJPA(_entityManager -> {
                            Book _post = _entityManager.find(Book.class, 1L);
                            _post.setTitle("High-Performance Java Persistence, 2nd edition");

                            LOGGER.info("Bob updates the book record");
                            _entityManager.flush();
                        });
                    } catch (Exception e) {
                        LOGGER.error("Bob's optimistic locking failure: ", e);
                    }
                });
            });
        } catch (Exception e) {
            LOGGER.error("Alice's optimistic locking failure: ", e);
        }
    }

    @Entity(name = "Book")
    @Table(name = "book")
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
