package com.vladmihalcea.book.hpjp.hibernate.type.json;

import com.fasterxml.jackson.databind.JsonNode;
import com.vladmihalcea.book.hpjp.util.AbstractPostgreSQLIntegrationTest;
import org.hibernate.Session;
import org.hibernate.annotations.NaturalId;
import org.hibernate.annotations.Type;
import org.hibernate.annotations.TypeDef;
import org.junit.Test;

import javax.persistence.*;

import static org.junit.Assert.assertEquals;

/**
 * @author Vlad Mihalcea
 */
public class PostgreSQLJsonNodeBinaryTypeTest extends AbstractPostgreSQLIntegrationTest {

    @Override
    protected Class<?>[] entities() {
        return new Class<?>[]{
                Book.class
        };
    }

    @Override
    protected void afterInit() {
        doInJPA(entityManager -> {

            Book book = new Book();
            book.setIsbn("978-9730228236");
            book.setProperties(
                JacksonUtil.toJsonNode(
                    "{" +
                    "   \"title\": \"High-Performance Java Persistence\"," +
                    "   \"author\": \"Vlad Mihalcea\"," +
                    "   \"publisher\": \"Amazon\"," +
                    "   \"price\": 44.99" +
                    "}"
                )
            );

            entityManager.persist(book);
        });
    }

    @Test
    public void testFetchAndUpdate() {

        doInJPA(entityManager -> {
            Book book = entityManager
                    .unwrap(Session.class)
                    .bySimpleNaturalId(Book.class)
                    .load("978-9730228236");

            assertEquals("High-Performance Java Persistence", book.getProperties().get("title").asText());

            LOGGER.info("Book details: {}", book.getProperties());

            book.setProperties(
                    JacksonUtil.toJsonNode(
                            "{" +
                                    "   \"title\": \"High-Performance Java Persistence\"," +
                                    "   \"author\": \"Vlad Mihalcea\"," +
                                    "   \"publisher\": \"Amazon\"," +
                                    "   \"price\": 44.99," +
                                    "   \"url\": \"https://www.amazon.com/High-Performance-Java-Persistence-Vlad-Mihalcea/dp/973022823X/\"" +
                                    "}"
                    )
            );
        });
    }

    @Test
    public void testFetchUsingJPQL() {
        doInJPA(entityManager -> {
            JsonNode properties = entityManager
            .createQuery(
                "select b.properties " +
                "from Book b " +
                "where b.isbn = :isbn", JsonNode.class)
            .setParameter("isbn", "978-9730228236")
            .getSingleResult();

            assertEquals("High-Performance Java Persistence", properties.get("title").asText());
        });
    }

    @Entity(name = "Book")
    @Table(name = "book")
    @TypeDef(name = "jsonb-node", typeClass = JsonNodeBinaryType.class)
    public static class Book {

        @Id
        @GeneratedValue
        private Long id;

        @NaturalId
        private String isbn;

        @Type(type = "jsonb-node")
        @Column(columnDefinition = "jsonb")
        private JsonNode properties;

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

        public JsonNode getProperties() {
            return properties;
        }

        public void setProperties(JsonNode properties) {
            this.properties = properties;
        }
    }
}
