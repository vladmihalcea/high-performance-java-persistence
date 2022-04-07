package com.vladmihalcea.book.hpjp.hibernate.concurrency.upsert;

import com.vladmihalcea.book.hpjp.util.AbstractSQLServerIntegrationTest;
import org.hibernate.annotations.NaturalId;
import org.junit.Test;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.LockModeType;
import jakarta.persistence.Table;
import java.util.concurrent.CountDownLatch;

/**
 * @author Vlad Mihalcea
 */
public class SQLServerUpsertAlternativeTest extends AbstractSQLServerIntegrationTest {

    @Override
    protected Class<?>[] entities() {
        return new Class<?>[]{
            Book.class
        };
    }

    @Test
    public void testUpsertWithInsertAndUpdateLock() {
        CountDownLatch aliceCountDownLatch = new CountDownLatch(1);

        doInJPA(entityManager -> {
            Book book = new Book();
            book.setId(1L);
            book.setTitle("Book 1");

            entityManager.persist(book);
            entityManager.flush();

            executeAsync(() -> {
                doInJPA(_entityManager -> {
                    try {
                        LOGGER.info("Bob tries to lock the record");
                        Book _book = _entityManager.find(Book.class, 1L, LockModeType.PESSIMISTIC_READ);
                        if (_book == null) {
                            LOGGER.info("Bob inserts");
                            _book = new Book();
                            _book.setId(1L);
                            _book.setTitle("Book 2");
                            _entityManager.persist(_book);
                        } else {
                            LOGGER.info("Bob updates");
                            _book.setTitle("Book 2");
                        }
                    } catch (Exception e) {
                        e.printStackTrace();
                    } finally {
                        aliceCountDownLatch.countDown();
                    }
                });
            });

            LOGGER.info("Alice starts sleeping");
            sleep(5000);
            LOGGER.info("Alice woke up and releases the logs after she commits");
        });

        LOGGER.info("");
        awaitOnLatch(aliceCountDownLatch);
    }


    @Entity(name = "Book")
    @Table(name = "book")
    public static class Book {

        @Id
        private Long id;

        @NaturalId
        private String isbn;

        private String title;

        public Long getId() {
            return id;
        }

        public void setId(Long id) {
            this.id = id;
        }

        public String getIsbn() {
            return isbn;
        }

        public void setIsbn(String isbn) {
            this.isbn = isbn;
        }

        public String getTitle() {
            return title;
        }

        public void setTitle(String title) {
            this.title = title;
        }
    }
}
