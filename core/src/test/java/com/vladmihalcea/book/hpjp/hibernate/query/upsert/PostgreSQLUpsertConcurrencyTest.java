package com.vladmihalcea.book.hpjp.hibernate.query.upsert;

import com.vladmihalcea.book.hpjp.util.AbstractMySQLIntegrationTest;
import com.vladmihalcea.book.hpjp.util.AbstractTest;
import com.vladmihalcea.book.hpjp.util.exception.ExceptionUtil;
import com.vladmihalcea.book.hpjp.util.providers.Database;
import org.hibernate.Session;
import org.hibernate.annotations.NaturalId;
import org.junit.Test;

import jakarta.persistence.*;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 * @author Vlad Mihalcea
 */
public class PostgreSQLUpsertConcurrencyTest extends AbstractTest {

    @Override
    protected Class<?>[] entities() {
        return new Class<?>[] {
            Book.class,
        };
    }

    @Override
    protected Database database() {
        return Database.POSTGRESQL;
    }

    private final CountDownLatch aliceLatch = new CountDownLatch(1);

    @Test
    public void testTimeoutOnSecondTransaction() {
        doInJPA(entityManager -> {
            entityManager.createNativeQuery(
                "INSERT INTO book (" +
                "   id, " +
                "   title, " +
                "   isbn" +
                ") " +
                "VALUES (" +
                "   :id, " +
                "   :title, " +
                "   :isbn" +
                ") " +
                "ON CONFLICT (id) DO NOTHING")
            .setParameter("id", 1L)
            .setParameter("title", "High-Performance Hibernate")
            .setParameter("isbn", "978-9730228236")
            .executeUpdate();

            final AtomicBoolean preventedByLocking = new AtomicBoolean();
            final AtomicInteger bobUpdateCount = new AtomicInteger();

            executeAsync(() -> {
                try {
                    doInJPA(_entityManager -> {
                        _entityManager.unwrap(Session.class).doWork(this::setJdbcTimeout);

                        int updateCount = _entityManager.createNativeQuery(
                            "INSERT INTO book (" +
                            "   id, " +
                            "   title, " +
                            "   isbn" +
                            ") " +
                            "VALUES (" +
                            "   :id, " +
                            "   :title, " +
                            "   :isbn" +
                            ") " +
                            "ON CONFLICT (id) DO NOTHING")
                        .setParameter("id", 1L)
                        .setParameter("title", "High-Performance Hibernate")
                        .setParameter("isbn", "978-9730228236")
                        .executeUpdate();

                        bobUpdateCount.set(updateCount);
                    });
                } catch (Exception e) {
                    if( ExceptionUtil.isLockTimeout( e )) {
                        preventedByLocking.set( true );
                    }
                }

                aliceLatch.countDown();
            });

            awaitOnLatch(aliceLatch);

            assertTrue(preventedByLocking.get());
            assertEquals(0, bobUpdateCount.get());
        });
    }

    @Test
    public void testResumeOnSecondTransaction() {
        doInJPA(entityManager -> {
            entityManager.createNativeQuery(
                "INSERT INTO book (" +
                "   id, " +
                "   title, " +
                "   isbn" +
                ") " +
                "VALUES (" +
                "   :id, " +
                "   :title, " +
                "   :isbn" +
                ") " +
                "ON CONFLICT (id) DO NOTHING")
            .setParameter("id", 1L)
            .setParameter("title", "High-Performance Hibernate")
            .setParameter("isbn", "978-9730228236")
            .executeUpdate();

            executeAsync(() -> {
                doInJPA(_entityManager -> {
                    LOGGER.info("Bob tries to insert the same record");
                    int updateCount = _entityManager.createNativeQuery(
                        "INSERT INTO book (" +
                            "   id, " +
                            "   title, " +
                            "   isbn" +
                            ") " +
                            "VALUES (" +
                            "   :id, " +
                            "   :title, " +
                            "   :isbn" +
                            ") " +
                            "ON CONFLICT (id) DO NOTHING")
                        .setParameter("id", 1L)
                        .setParameter("title", "High-Performance Hibernate")
                        .setParameter("isbn", "978-9730228236")
                        .executeUpdate();

                    LOGGER.info("Bob managed to execute the UPSERT statement, and the update count is {}", updateCount);

                    aliceLatch.countDown();
                });
            });

            LOGGER.info("Alice starts waiting for 3 seconds!");
            sleep(TimeUnit.SECONDS.toMillis(3));
        });
        LOGGER.info("Alice's transaction has committed!");

        awaitOnLatch(aliceLatch);
    }

    @Entity(name = "Book")
    @Table(name = "book")
    public static class Book {

        @Id
        private Long id;

        private String title;

        @NaturalId
        private String isbn;

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

        public String getIsbn() {
            return isbn;
        }

        public void setIsbn(String isbn) {
            this.isbn = isbn;
        }
    }
}
