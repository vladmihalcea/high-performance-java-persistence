package com.vladmihalcea.book.hpjp.hibernate.identifier;

import com.vladmihalcea.book.hpjp.util.AbstractTest;
import org.hibernate.Session;
import org.hibernate.annotations.GenericGenerator;
import org.junit.Test;

import javax.persistence.*;
import java.sql.Statement;

import static org.junit.Assert.assertEquals;

public class AssignedIdentifierTest extends AbstractTest {

    @Override
    protected Class<?>[] entities() {
        return new Class<?>[] {
            Book.class,
        };
    }

    @Test
    public void test() {
        doInJPA(entityManager -> {
            Book book = new Book();
            book.setIsbn(9789730228236L);
            book.setTitle("High-Performance Java Persistence");
            book.setAuthor("Vlad Mihalcea");

            entityManager.persist(book);
        });
        doInJPA(entityManager -> {
            Book book = entityManager.find(Book.class, 9789730228236L);
            assertEquals("High-Performance Java Persistence", book.getTitle());
        });
    }

    @Entity(name = "Book")
    public static class Book {

        @Id
        private Long isbn;

        private String title;

        private String author;

        //Getters and setters omitted for brevity

        public Long getIsbn() {
            return isbn;
        }

        public void setIsbn(Long isbn) {
            this.isbn = isbn;
        }

        public String getTitle() {
            return title;
        }

        public void setTitle(String title) {
            this.title = title;
        }

        public String getAuthor() {
            return author;
        }

        public void setAuthor(String author) {
            this.author = author;
        }
    }

}
