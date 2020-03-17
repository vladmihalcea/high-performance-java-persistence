package com.vladmihalcea.book.hpjp.hibernate.equality;

import com.vladmihalcea.book.hpjp.hibernate.identifier.Identifiable;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.hibernate.annotations.NaturalId;
import org.junit.Test;

import javax.persistence.*;
import java.util.Objects;

/**
 * @author Sergei Egorov
 */
public class LombokEqualityTest
        extends AbstractEqualityCheckTest<LombokEqualityTest.Book> {

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
    @Data
    @EqualsAndHashCode(of = "isbn")
    public static class Book implements Identifiable<Long> {

        @Id
        @GeneratedValue
        private Long id;

        private String title;

        @NaturalId
        @Column(nullable = false)
        private String isbn;
    }
}
