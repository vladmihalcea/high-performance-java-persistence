package com.vladmihalcea.hpjp.hibernate.pc;

import com.vladmihalcea.hpjp.util.AbstractTest;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertNotSame;

public class SaveVariantsTest extends AbstractTest {

    @Override
    protected Class<?>[] entities() {
        return new Class<?>[]{
                Book.class
        };
    }

    @Test
    public void testPersist() {
        doInJPA(entityManager -> {
            Book book = new Book()
            .setIsbn("978-9730228236")
            .setTitle("High-Performance Java Persistence")
            .setAuthor("Vlad Mihalcea");

            entityManager.persist(book);

            LOGGER.info("Persisting the Book entity with the id: {}", book.getId());
        });
    }

    @Test
    public void testMerge() {
        Book _book = doInJPA(entityManager -> {
            Book book = new Book()
            .setIsbn("978-9730228236")
            .setTitle("High-Performance Java Persistence")
            .setAuthor("Vlad Mihalcea");

            entityManager.persist(book);

            return book;
        });

        LOGGER.info("Modifying the Book entity");

        _book.setTitle("High-Performance Java Persistence, 2nd edition");

        doInJPA(entityManager -> {
            Book book = entityManager.merge(_book);

            LOGGER.info("Merging the Book entity");

            assertNotSame(book, _book);
        });
    }

    @Test
    public void testMergeAlreadyManaged() {
        Book _book = doInJPA(entityManager -> {
            Book book = new Book()
            .setIsbn("978-9730228236")
            .setTitle("High-Performance Java Persistence")
            .setAuthor("Vlad Mihalcea");

            entityManager.persist(book);

            return book;
        });

        _book.setTitle("High-Performance Java Persistence, 2nd edition");

        doInJPA(entityManager -> {
            Book book = entityManager.find(Book.class, _book.getId());

            entityManager.merge(_book);
        });
    }

    @Entity(name = "Book")
    @Table(name = "book")
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
