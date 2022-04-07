package com.vladmihalcea.book.hpjp.hibernate.criteria.literal;

import com.vladmihalcea.book.hpjp.util.AbstractTest;
import com.vladmihalcea.book.hpjp.util.providers.Database;
import org.hibernate.annotations.NaturalId;
import org.junit.Test;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Root;

import static org.junit.Assert.assertEquals;

/**
 * @author Vlad Mihalcea
 */
public class DefaultCriteriaLiteralTest extends AbstractTest {

    @Override
    protected Class<?>[] entities() {
        return new Class<?>[] {
            Book.class
        };
    }

    @Override
    protected Database database() {
        return Database.POSTGRESQL;
    }

    @Test
    public void test() {
        doInJPA(entityManager -> {
            Book book = new Book();
            book.setId(1L);
            book.setName("High-Performance Java Persistence");
            book.setIsbn(978_9730228236L);
            book.setActive(true);

            entityManager.persist(book);
        });

        doInJPA(entityManager -> {
            CriteriaBuilder cb = entityManager.getCriteriaBuilder();

            CriteriaQuery<Book> cq = cb.createQuery(Book.class);
            Root<Book> root = cq.from(Book.class);
            cq.select(root);
            cq.where(cb.equal(root.get("isbn"), 978_9730228236L));

            Book book = entityManager.createQuery(cq).getSingleResult();
            assertEquals("High-Performance Java Persistence", book.getName());
        });

        doInJPA(entityManager -> {
            CriteriaBuilder cb = entityManager.getCriteriaBuilder();

            CriteriaQuery<Book> cq = cb.createQuery(Book.class);
            Root<Book> root = cq.from(Book.class);
            cq.select(root);
            cq.where(cb.equal(root.get("name"), "High-Performance Java Persistence"));

            Book book = entityManager.createQuery(cq).getSingleResult();
            assertEquals(978_9730228236L, book.getIsbn());
        });

        doInJPA(entityManager -> {
            CriteriaBuilder cb = entityManager.getCriteriaBuilder();

            CriteriaQuery<Book> cq = cb.createQuery(Book.class);
            Root<Book> root = cq.from(Book.class);
            cq.select(root);
            cq.where(cb.equal(root.get("active"), true));

            Book book = entityManager.createQuery(cq).getSingleResult();
            assertEquals(978_9730228236L, book.getIsbn());
        });
    }

    @Entity(name = "Book")
    @Table(name = "book")
    public static class Book {

        @Id
        private Long id;

        private String name;

        @NaturalId
        private long isbn;

        private boolean active;

        public Long getId() {
            return id;
        }

        public void setId(Long id) {
            this.id = id;
        }

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public long getIsbn() {
            return isbn;
        }

        public void setIsbn(long isbn) {
            this.isbn = isbn;
        }

        public boolean isActive() {
            return active;
        }

        public void setActive(boolean active) {
            this.active = active;
        }
    }
}
