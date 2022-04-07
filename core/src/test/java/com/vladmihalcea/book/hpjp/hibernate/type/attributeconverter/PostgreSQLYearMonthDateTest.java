package com.vladmihalcea.book.hpjp.hibernate.type.attributeconverter;

import com.vladmihalcea.book.hpjp.util.AbstractPostgreSQLIntegrationTest;
import org.hibernate.Session;
import org.hibernate.annotations.NaturalId;
import org.junit.Test;

import jakarta.persistence.*;
import java.time.Instant;
import java.time.YearMonth;
import java.time.ZoneId;

import static org.junit.Assert.assertEquals;

/**
 * @author Vlad Mihalcea
 */
public class PostgreSQLYearMonthDateTest extends AbstractPostgreSQLIntegrationTest {

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
            book.setPublishedOn(YearMonth.of(2016, 10));

            entityManager.persist(book);
        });

        doInJPA(entityManager -> {
            Book book = entityManager
                    .unwrap(Session.class)
                    .bySimpleNaturalId(Book.class)
                    .load("978-9730228236");

            assertEquals(YearMonth.of(2016, 10), book.getPublishedOn());
        });

        doInJPA(entityManager -> {
            Book book = entityManager.createQuery("""
                select b
                from Book b
                where
                   b.title = :title and
                   b.publishedOn = :publishedOn
                """, Book.class)
            .setParameter("title", "High-Performance Java Persistence")
            .setParameter("publishedOn", YearMonth.of(2016, 10))
            .getSingleResult();

            assertEquals("978-9730228236", book.getIsbn());
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

        @Column(name = "published_on", columnDefinition = "date")
        @Convert(converter = YearMonthDateAttributeConverter.class)
        private YearMonth publishedOn;

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

        public YearMonth getPublishedOn() {
            return publishedOn;
        }

        public void setPublishedOn(YearMonth publishedOn) {
            this.publishedOn = publishedOn;
        }
    }

    public static class YearMonthDateAttributeConverter
            implements AttributeConverter<YearMonth, java.sql.Date> {

        @Override
        public java.sql.Date convertToDatabaseColumn(YearMonth attribute) {
            if (attribute != null) {
                return java.sql.Date.valueOf(attribute.atDay(1));
            }
            return null;
        }

        @Override
        public YearMonth convertToEntityAttribute(java.sql.Date dbData) {
            if (dbData != null) {
                return YearMonth.from(Instant.ofEpochMilli(dbData.getTime())
                        .atZone(ZoneId.systemDefault())
                        .toLocalDate());
            }
            return null;
        }
    }
}
