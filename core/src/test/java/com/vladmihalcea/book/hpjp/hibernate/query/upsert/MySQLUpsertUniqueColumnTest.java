package com.vladmihalcea.book.hpjp.hibernate.query.upsert;

import com.vladmihalcea.book.hpjp.hibernate.identifier.composite.CompositeIdIdentityGeneratedTest;
import com.vladmihalcea.book.hpjp.util.AbstractMySQLIntegrationTest;
import com.vladmihalcea.book.hpjp.util.AbstractTest;
import com.vladmihalcea.book.hpjp.util.exception.ExceptionUtil;
import org.hibernate.Session;
import org.hibernate.annotations.NaturalId;
import org.hibernate.annotations.SQLInsert;
import org.junit.Test;

import jakarta.persistence.*;
import java.io.Serializable;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.junit.Assert.assertTrue;

/**
 * @author Vlad Mihalcea
 */
public class MySQLUpsertUniqueColumnTest extends AbstractMySQLIntegrationTest {

    @Override
    protected Class<?>[] entities() {
        return new Class<?>[] {
            Book.class,
        };
    }

    private final CountDownLatch aliceLatch = new CountDownLatch(1);

    @Test
    public void test() {
        doInJPA(entityManager -> {
            Book book = new Book();
            book.setTitle("High-Performance Java Persistence");
            book.setIsbn("978-9730228236");
            entityManager.persist(book);

            final AtomicBoolean preventedByLocking = new AtomicBoolean();

            executeAsync(() -> {
                try {
                    doInJPA(_entityManager -> {
                        _entityManager.unwrap(Session.class).doWork(this::setJdbcTimeout);
                        _entityManager.createNativeQuery(
                            "INSERT IGNORE " +
                            "INTO book (title, isbn) " +
                            "VALUES (:title, :isbn)")
                        .setParameter("title", "High-Performance Hibernate")
                        .setParameter("isbn", "978-9730228236")
                        .executeUpdate();
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
        });
    }

    @Entity(name = "Book")
    @Table(name = "book")
    public static class Book {

        @Id
        @GeneratedValue(strategy = GenerationType.IDENTITY)
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
