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
public class HibernateUpsertOnConflictTest extends AbstractTest {

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
        doInJPA(entityManager -> {
            entityManager.createQuery("""
                insert into Book (id, title, isbn)
                values (
                    :id,
                    :title,
                    :isbn
                )
                on conflict(id) do
                update
                set
                    title = excluded.title,
                    isbn = excluded.isbn
                """)
            .setParameter("id", 1L)
            .setParameter("title", "High-Performance Java Persistence")
            .setParameter("isbn", "978-9730228236")
            .executeUpdate();
        });

        doInJPA(entityManager -> {
            entityManager.createQuery("""
                insert into Book (id, title, isbn)
                values (
                    :id,
                    :title,
                    :isbn
                )
                on conflict(id) do
                update
                set
                    title = excluded.title,
                    isbn = excluded.isbn
                """)
            .setParameter("id", 1L)
            .setParameter("title", "High-Performance Java Persistence, 2nd edition")
            .setParameter("isbn", "978-9730228237")
            .executeUpdate();
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
