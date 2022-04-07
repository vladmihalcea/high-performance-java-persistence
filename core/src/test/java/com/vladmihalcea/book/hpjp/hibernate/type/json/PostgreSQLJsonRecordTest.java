package com.vladmihalcea.book.hpjp.hibernate.type.json;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.vladmihalcea.book.hpjp.util.AbstractPostgreSQLIntegrationTest;
import com.vladmihalcea.hibernate.type.json.JsonBinaryType;
import jakarta.persistence.*;
import org.hibernate.Session;
import org.hibernate.annotations.NaturalId;
import org.hibernate.annotations.Type;
import org.junit.Test;

import java.io.Serializable;
import java.net.URL;

import static org.junit.Assert.assertEquals;

/**
 * @author Vlad Mihalcea
 */
public class PostgreSQLJsonRecordTest extends AbstractPostgreSQLIntegrationTest {

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
                    .setIsbn("978-9730228236")
                    .setProperties(
                        new BookRecord(
                            "High-Performance Java Persistence",
                            "Vlad Mihalcea",
                            "Amazon",
                            4499L,
                            null
                        )
                    )
            );
        });
    }

    @Test
    public void testFetchAndUpdate() {

        doInJPA(entityManager -> {
            Book book = entityManager
                .unwrap(Session.class)
                .bySimpleNaturalId(Book.class)
                .load("978-9730228236");

            BookRecord bookRecord = book.getProperties();

            assertEquals(
                "High-Performance Java Persistence",
                bookRecord.title()
            );
            assertEquals(
                "Vlad Mihalcea",
                bookRecord.author()
            );

            LOGGER.info("Book details: {}", book.getProperties());

            book.setProperties(
                new BookRecord(
                    bookRecord.title(),
                    bookRecord.author(),
                    bookRecord.publisher(),
                    bookRecord.priceInCents(),
                    urlValue("https://www.amazon.com/dp/973022823X/")
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

        @Type(JsonBinaryType.class)
        @Column(columnDefinition = "jsonb")
        private BookRecord properties;

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

        public BookRecord getProperties() {
            return properties;
        }

        public Book setProperties(BookRecord properties) {
            this.properties = properties;
            return this;
        }
    }

    @JsonAutoDetect(fieldVisibility = JsonAutoDetect.Visibility.ANY)
    public static record BookRecord (
        String title,
        String author,
        String publisher,
        Long priceInCents,
        URL url
    ) implements Serializable {
        @JsonCreator
        public BookRecord(
            @JsonProperty("title") String title,
            @JsonProperty("author") String author,
            @JsonProperty("publisher") String publisher,
            @JsonProperty("priceInCents") String priceInCents,
            @JsonProperty("url") String url) {
            this(
                title,
                author,
                publisher,
                longValue(priceInCents),
                urlValue(url)
            );
        }
    }
}
