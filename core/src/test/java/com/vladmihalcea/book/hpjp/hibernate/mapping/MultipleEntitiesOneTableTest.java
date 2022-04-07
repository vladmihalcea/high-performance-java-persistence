package com.vladmihalcea.book.hpjp.hibernate.mapping;

import com.fasterxml.jackson.databind.node.ObjectNode;
import com.vladmihalcea.book.hpjp.util.AbstractPostgreSQLIntegrationTest;
import com.vladmihalcea.hibernate.type.json.JsonBinaryType;
import com.vladmihalcea.hibernate.type.json.internal.JacksonUtil;
import jakarta.persistence.*;
import org.hibernate.Session;
import org.hibernate.annotations.DynamicUpdate;
import org.hibernate.annotations.NaturalId;
import org.hibernate.annotations.Type;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

/**
 * @author Vlad Mihalcea
 */
public class MultipleEntitiesOneTableTest extends AbstractPostgreSQLIntegrationTest {

    @Override
    protected Class<?>[] entities() {
        return new Class<?>[]{
            Book.class,
            BookSummary.class
        };
    }

    @Override
    protected void afterInit() {
        doInJPA(entityManager -> {
            entityManager.persist(
                new Book()
                    .setIsbn("978-9730228236")
                    .setTitle("High-Performance Java Persistence")
                    .setAuthor("Vlad Mihalcea")
                    .setProperties(
                        "{" +
                            "   \"publisher\": \"Amazon\"," +
                            "   \"price\": 44.99," +
                            "   \"publication_date\": \"2016-20-12\"," +
                            "   \"dimensions\": \"8.5 x 1.1 x 11 inches\"," +
                            "   \"weight\": \"2.5 pounds\"," +
                            "   \"average_review\": \"4.7 out of 5 stars\"," +
                            "   \"url\": \"https://amzn.com/973022823X\"" +
                        "}"
                    )
            );

            entityManager.persist(
                new BookSummary()
                    .setIsbn("978-1934356555")
                    .setTitle("SQL Antipatterns")
                    .setAuthor("Bill Karwin")
            );
        });
    }

    @Test
    public void test() {
        doInJPA(entityManager -> {
            BookSummary bookSummary = entityManager
                .unwrap(Session.class)
                .bySimpleNaturalId(BookSummary.class)
                .load("978-9730228236");

            assertEquals("High-Performance Java Persistence", bookSummary.getTitle());

            bookSummary.setTitle("High-Performance Java Persistence, 2nd edition");
        });

        doInJPA(entityManager -> {
            Book book = entityManager
                .unwrap(Session.class)
                .bySimpleNaturalId(Book.class)
                .load("978-9730228236");


            assertEquals("High-Performance Java Persistence, 2nd edition", book.getTitle());

            ObjectNode jsonProperties = book.getJsonProperties();
            assertEquals("4.7 out of 5 stars", jsonProperties.get("average_review").asText());

            jsonProperties.put("average_review", "4.8 out of 5 stars");
            book.setProperties(JacksonUtil.toString(jsonProperties));
        });
    }

    @Entity(name = "Book")
    @Table(name = "book")
    @DynamicUpdate
    public static class Book extends BaseBook<Book> {

        @Type(JsonBinaryType.class)
        @Column(columnDefinition = "jsonb")
        private String properties;

        public String getProperties() {
            return properties;
        }

        public Book setProperties(String properties) {
            this.properties = properties;
            return this;
        }

        public ObjectNode getJsonProperties() {
            return (ObjectNode) JacksonUtil.toJsonNode(properties);
        }
    }

    @Entity(name = "BookSummary")
    @Table(name = "book")
    public static class BookSummary extends BaseBook<BookSummary> {

    }

    @MappedSuperclass
    public static abstract class BaseBook<T extends BaseBook> {

        @Id
        @GeneratedValue
        private Long id;

        @NaturalId
        @Column(length = 15)
        private String isbn;

        @Column(length = 50)
        private String title;

        @Column(length = 50)
        private String author;

        public Long getId() {
            return id;
        }

        public T setId(Long id) {
            this.id = id;
            return (T) this;
        }

        public String getIsbn() {
            return isbn;
        }

        public T setIsbn(String isbn) {
            this.isbn = isbn;
            return (T) this;
        }

        public String getTitle() {
            return title;
        }

        public T setTitle(String title) {
            this.title = title;
            return (T) this;
        }

        public String getAuthor() {
            return author;
        }

        public T setAuthor(String author) {
            this.author = author;
            return (T) this;
        }
    }
}
