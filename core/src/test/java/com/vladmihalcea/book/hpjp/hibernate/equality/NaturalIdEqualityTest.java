package com.vladmihalcea.book.hpjp.hibernate.equality;

import com.vladmihalcea.book.hpjp.hibernate.identifier.Identifiable;
import org.hibernate.annotations.NaturalId;
import org.junit.Test;

import jakarta.persistence.*;
import java.util.Objects;

/**
 * @author Vlad Mihalcea
 */
public class NaturalIdEqualityTest
        extends AbstractEqualityCheckTest<NaturalIdEqualityTest.Book> {

    @Override
    protected Class<?>[] entities() {
        return new Class[] {
            Book.class
        };
    }

    @Test
    public void testEquality() {
        Book book = new Book();
        book.setTitle("High-PerformanceJava Persistence");
        book.setIsbn("123-456-7890");

        assertEqualityConsistency(Book.class, book);
    }

    @Entity(name = "Book")
    @Table(name = "book")
    public static class Book implements Identifiable<Long> {

        @Id
        @GeneratedValue
        private Long id;

        private String title;

        @NaturalId
        @Column(nullable = false)
        private String isbn;

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (!(o instanceof Book)) return false;

            Book book = (Book) o;
            return Objects.equals(getIsbn(), book.getIsbn());
        }

        @Override
        public int hashCode() {
            return Objects.hash(getIsbn());
        }

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
