package com.vladmihalcea.book.hpjp.hibernate.type.json;

import com.fasterxml.jackson.databind.JsonNode;
import com.vladmihalcea.book.hpjp.util.AbstractPostgreSQLIntegrationTest;
import com.vladmihalcea.book.hpjp.util.exception.ExceptionUtil;
import com.vladmihalcea.hibernate.type.json.JsonBinaryType;
import org.hibernate.Session;
import org.hibernate.annotations.NaturalId;
import org.hibernate.annotations.TypeDef;
import org.junit.Test;

import javax.persistence.*;
import java.io.Serializable;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.junit.Assert.*;

/**
 * @author Vlad Mihalcea
 */
public class PostgreSQLJsonBinaryTypeNativeTest extends AbstractPostgreSQLIntegrationTest {

    @Override
    protected Class<?>[] entities() {
        return new Class<?>[]{
            Book.class
        };
    }

    @Override
    protected void afterInit() {
        doInJPA(entityManager -> {
            entityManager.persist(
                new Book()
                    .setId(1L)
                    .setProperties(
                        new BookProperties("978-9730228236")
                    )
            );
        });
    }

    @Test
    public void testUpdateUsingNativeSQL() {

        doInJPA(entityManager -> {
            Book book = entityManager.find(Book.class, 1L);

            assertTrue(book.getProperties().getReviews().isEmpty());

            int updateCount = entityManager.createNativeQuery("""
                UPDATE 
                    book
                SET 
                    properties = :properties
                WHERE 
                    properties ->> 'isbn' = :isbn
                """)
                .setParameter("isbn", "978-9730228236")
                .unwrap(org.hibernate.query.Query.class)
                .setParameter(
                    "properties",
                    new BookProperties("978-9730228236")
                        .setTitle("High-Performance Java Persistence")
                        .setAuthor("Vlad Mihalcea")
                        .setPrice(new BigDecimal("44.99"))
                        .setPublisher("Amazon")
                        .setReviews(
                            List.of(
                                "Excellent book to understand Java Persistence",
                                "The best JPA ORM book out there"
                            )
                        )
                    , JsonBinaryType.INSTANCE
                )
                .executeUpdate();

            entityManager.refresh(book);

            assertEquals(2, book.getProperties().getReviews().size());
        });
    }

    @Test
    public void testUpdateUsingNativeSQLFails() {

        try {
            doInJPA(entityManager -> {
                Book book = entityManager.find(Book.class, 1L);

                assertTrue(book.getProperties().getReviews().isEmpty());

                int updateCount = entityManager.createNativeQuery("""
                    UPDATE 
                        book
                    SET 
                        properties = :properties
                    WHERE 
                        properties ->> 'isbn' = :isbn
                    """)
                    .setParameter("isbn", "978-9730228236")
                    .setParameter(
                        "properties",
                        new BookProperties("978-9730228236")
                            .setTitle("High-Performance Java Persistence")
                            .setAuthor("Vlad Mihalcea")
                            .setPrice(new BigDecimal("44.99"))
                            .setPublisher("Amazon")
                            .setReviews(
                                List.of(
                                    "Excellent book to understand Java Persistence",
                                    "The best JPA ORM book out there"
                                )
                            )
                    )
                    .executeUpdate();
            });

            fail("Failure expected!");
        } catch (Exception e) {
            Exception rootCause = ExceptionUtil.rootCause(e);
            assertTrue(rootCause.getMessage().contains("column \"properties\" is of type jsonb but expression is of type bytea"));
        }
    }

    @Entity(name = "Book")
    @Table(name = "book")
    @TypeDef(typeClass = JsonBinaryType.class, defaultForType = BookProperties.class)
    public static class Book {

        @Id
        private Long id;

        @Column(columnDefinition = "jsonb")
        private BookProperties properties;

        public Long getId() {
            return id;
        }

        public Book setId(Long id) {
            this.id = id;
            return this;
        }

        public BookProperties getProperties() {
            return properties;
        }

        public Book setProperties(BookProperties properties) {
            this.properties = properties;
            return this;
        }
    }

    public static class BookProperties implements Serializable {

        private String isbn;

        private String title;

        private String author;

        private String publisher;

        private BigDecimal price;

        private List<String> reviews = new ArrayList<>();

        private BookProperties() {
        }

        public BookProperties(String isbn) {
            this.isbn = isbn;
        }

        public String getIsbn() {
            return isbn;
        }

        public BookProperties setIsbn(String isbn) {
            this.isbn = isbn;
            return this;
        }

        public String getTitle() {
            return title;
        }

        public BookProperties setTitle(String title) {
            this.title = title;
            return this;
        }

        public String getAuthor() {
            return author;
        }

        public BookProperties setAuthor(String author) {
            this.author = author;
            return this;
        }

        public String getPublisher() {
            return publisher;
        }

        public BookProperties setPublisher(String publisher) {
            this.publisher = publisher;
            return this;
        }

        public BigDecimal getPrice() {
            return price;
        }

        public BookProperties setPrice(BigDecimal price) {
            this.price = price;
            return this;
        }

        public List<String> getReviews() {
            return reviews;
        }

        public BookProperties setReviews(List<String> reviews) {
            this.reviews = reviews;
            return this;
        }
    }
}
