package com.vladmihalcea.book.hpjp.hibernate.type.attributeconverter;

import com.vladmihalcea.book.hpjp.util.AbstractPostgreSQLIntegrationTest;
import org.hibernate.Session;
import org.hibernate.annotations.NaturalId;
import org.hibernate.boot.MetadataBuilder;
import org.hibernate.boot.spi.MetadataBuilderContributor;
import org.hibernate.jpa.boot.spi.TypeContributorList;
import org.junit.Ignore;
import org.junit.Test;

import jakarta.persistence.*;
import java.time.YearMonth;
import java.util.Collections;
import java.util.Properties;

import static org.junit.Assert.assertEquals;

/**
 * @author Vlad Mihalcea
 */
public class PostgreSQLYearMonthIntegerConverterAutoApplyTest extends AbstractPostgreSQLIntegrationTest {

    @Override
    protected Class<?>[] entities() {
        return new Class<?>[]{
                Book.class
        };
    }

    @Override
    protected void additionalProperties(Properties properties) {
        properties.put("hibernate.metadata_builder_contributor",
            (MetadataBuilderContributor) metadataBuilder ->
                    metadataBuilder.applyAttributeConverter(YearMonthIntegerAttributeConverter.class)
        );
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

        @Column(name = "published_on", columnDefinition = "integer")
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

    @Converter(autoApply = true)
    public static class YearMonthIntegerAttributeConverter
            implements AttributeConverter<YearMonth, Integer> {

        @Override
        public Integer convertToDatabaseColumn(YearMonth attribute) {
            if (attribute != null) {
                return (attribute.getYear() * 100) + attribute.getMonth().getValue();
            }
            return null;
        }

        @Override
        public YearMonth convertToEntityAttribute(Integer dbData) {
            if (dbData != null) {
                int year = dbData / 100;
                int month = dbData % 100;
                return YearMonth.of(year, month);
            }
            return null;
        }
    }
}
