package com.vladmihalcea.book.hpjp.hibernate.pc;

import com.vladmihalcea.book.hpjp.util.AbstractTest;
import org.hibernate.Session;
import org.hibernate.annotations.SelectBeforeUpdate;
import org.junit.Test;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

public class SaveVariantsSelectBeforeUpdateTest extends AbstractTest {

    @Override
    protected Class<?>[] entities() {
        return new Class<?>[]{
                Book.class
        };
    }

    @Test
    public void testUpdate() {
        Book _book = doInJPA(entityManager -> {
            Book book = new Book()
            .setIsbn("978-9730228236")
            .setTitle("High-Performance Java Persistence")
            .setAuthor("Vlad Mihalcea");

            entityManager.persist(book);

            return book;
        });

        doInJPA(entityManager -> {
            Session session = entityManager.unwrap(Session.class);

            session.update(_book);
        });
    }

    @Entity(name = "Book")
    @Table(name = "book")
    @SelectBeforeUpdate
    public static class Book {

        @Id
        @GeneratedValue
        private Long id;

        private String isbn;

        private String title;

        private String author;

        public Long getId() {
            return id;
        }

        public Book setId(Long id) {
            this.id = id;
            return this;
        }

        public String getIsbn() {
            return isbn;
        }

        public Book setIsbn(String isbn) {
            this.isbn = isbn;
            return this;
        }

        public String getTitle() {
            return title;
        }

        public Book setTitle(String title) {
            this.title = title;
            return this;
        }

        public String getAuthor() {
            return author;
        }

        public Book setAuthor(String author) {
            this.author = author;
            return this;
        }
    }
}
