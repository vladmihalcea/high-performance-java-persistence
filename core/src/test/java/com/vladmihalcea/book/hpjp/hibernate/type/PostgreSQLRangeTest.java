package com.vladmihalcea.book.hpjp.hibernate.type;

import com.vladmihalcea.book.hpjp.util.AbstractPostgreSQLIntegrationTest;
import com.vladmihalcea.hibernate.type.range.PostgreSQLRangeType;
import com.vladmihalcea.hibernate.type.range.Range;
import jakarta.persistence.*;
import org.hibernate.Session;
import org.hibernate.annotations.NaturalId;
import org.hibernate.annotations.Type;
import org.junit.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 * @author Vlad Mihalcea
 */
public class PostgreSQLRangeTest extends AbstractPostgreSQLIntegrationTest {

    @Override
    protected Class<?>[] entities() {
        return new Class<?>[]{
                Book.class
        };
    }

    @Test
    public void test() {
        doInJPA(entityManager -> {
            Book book = new Book();
            book.setIsbn("978-9730228236");
            book.setTitle("High-Performance Java Persistence");
            book.setPriceRange(
                Range.closed(
                    BigDecimal.valueOf(39.95d),
                    BigDecimal.valueOf(45.95d)
                )
            );
            book.setDiscountDateRange(
                Range.closedOpen(
                    LocalDate.of(2019, 11, 29),
                    LocalDate.of(2019, 12, 3)
                )
            );

            entityManager.persist(book);
        });

        doInJPA(entityManager -> {
            Book book = entityManager
            .unwrap(Session.class)
            .bySimpleNaturalId(Book.class)
            .load("978-9730228236");

            assertEquals(BigDecimal.valueOf(39.95d), book.getPriceRange().lower());
            assertEquals(BigDecimal.valueOf(45.95d), book.getPriceRange().upper());

            assertEquals(LocalDate.of(2019, 11, 29), book.getDiscountDateRange().lower());
            assertEquals(LocalDate.of(2019, 12, 3), book.getDiscountDateRange().upper());
        });

        doInJPA(entityManager -> {
            List<Book> discountedBooks = entityManager
            .createNativeQuery(
                    "SELECT * " +
                    "FROM book b " +
                    "WHERE " +
                    "   b.discount_date_range @> CAST(:today AS date) = true ", Book.class)
            .setParameter("today", LocalDate.of(2019, 12, 1))
            .getResultList();

            assertTrue(
                discountedBooks.stream().anyMatch(
                    book -> book.getTitle().equals("High-Performance Java Persistence")
                )
            );
        });
    }


    @Entity(name = "Book")
    @Table(name = "book")
    public static class Book {

        @Id
        @GeneratedValue
        private Long id;

        @NaturalId
        private String isbn;

        private String title;

        @Type(PostgreSQLRangeType.class)
        @Column(name = "price_cent_range", columnDefinition = "numrange")
        private Range<BigDecimal> priceRange;

        @Type(PostgreSQLRangeType.class)
        @Column(name = "discount_date_range", columnDefinition = "daterange")
        private Range<LocalDate> discountDateRange;

        public Long getId() {
            return id;
        }

        public void setId(Long id) {
            this.id = id;
        }

        public String getIsbn() {
            return isbn;
        }

        public void setIsbn(String isbn) {
            this.isbn = isbn;
        }

        public String getTitle() {
            return title;
        }

        public void setTitle(String title) {
            this.title = title;
        }

        public Range<BigDecimal> getPriceRange() {
            return priceRange;
        }

        public void setPriceRange(Range<BigDecimal> priceRange) {
            this.priceRange = priceRange;
        }

        public Range<LocalDate> getDiscountDateRange() {
            return discountDateRange;
        }

        public void setDiscountDateRange(Range<LocalDate> discountDateRange) {
            this.discountDateRange = discountDateRange;
        }
    }
}
