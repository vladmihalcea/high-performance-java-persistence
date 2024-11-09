package com.vladmihalcea.hpjp.hibernate.query.upsert;

import com.vladmihalcea.hpjp.util.AbstractTest;
import com.vladmihalcea.hpjp.util.exception.ExceptionUtil;
import com.vladmihalcea.hpjp.util.providers.Database;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import org.hibernate.annotations.NaturalId;
import org.junit.Test;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/**
 * @author Vlad Mihalcea
 */
public class HibernateUpsertMergeTest extends AbstractTest {

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

    @Test
    public void test() {
        doInStatelessSession(session -> {
            session.upsert(
                new Book()
                    .setId(1L)
                    .setTitle("High-Performance Hibernate")
                    .setIsbn("978-9730228236")
            );

            session.upsert(
                new Book()
                    .setId(1L)
                    .setTitle("High-Performance Hibernate 2nd edition")
                    .setIsbn("978-9730228236")
            );
        });
    }

    @Test
    public void testBatching() {
        doInStatelessSession(session -> {
            session.setJdbcBatchSize(50);

            session.upsert(
                new Book()
                    .setId(1L)
                    .setTitle("High-Performance Hibernate")
                    .setIsbn("978-9730228236")
            );

            session.upsert(
                new Book()
                    .setId(1L)
                    .setTitle("High-Performance Hibernate 2nd edition")
                    .setIsbn("978-9730228236")
            );
        });
    }

    @Test
    public void testTimeoutOnSecondTransaction() {
        CountDownLatch aliceLatch = new CountDownLatch(1);

        doInStatelessSession(session -> {
            session.upsert(
                new Book()
                    .setId(1L)
                    .setTitle("High-Performance Hibernate")
                    .setIsbn("978-9730228236")
            );

            final AtomicBoolean preventedByLocking = new AtomicBoolean();
            final AtomicBoolean bobUpdateSucceeded = new AtomicBoolean();

            executeAsync(() -> {
                try {
                    doInStatelessSession(_session -> {
                        _session.doWork(this::setJdbcTimeout);

                        _session.upsert(
                            new Book()
                                .setId(1L)
                                .setTitle("High-Performance Hibernate")
                                .setIsbn("978-9730228236")
                        );

                        bobUpdateSucceeded.set(true);
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
            assertFalse(bobUpdateSucceeded.get());
        });
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

        public String getIsbn() {
            return isbn;
        }

        public Book setIsbn(String isbn) {
            this.isbn = isbn;
            return this;
        }
    }
}
