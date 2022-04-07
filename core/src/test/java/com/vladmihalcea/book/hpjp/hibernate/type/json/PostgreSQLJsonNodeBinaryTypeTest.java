package com.vladmihalcea.book.hpjp.hibernate.type.json;

import com.fasterxml.jackson.databind.JsonNode;
import com.vladmihalcea.book.hpjp.util.AbstractPostgreSQLIntegrationTest;
import com.vladmihalcea.hibernate.type.json.JsonNodeBinaryType;
import com.vladmihalcea.hibernate.type.json.internal.JacksonUtil;
import jakarta.persistence.*;
import org.hibernate.Session;
import org.hibernate.annotations.NaturalId;
import org.hibernate.annotations.Type;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

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
            book.setProperties(JacksonUtil.toJsonNode("""
                {
                   "title": "High-Performance Java Persistence",
                   "author": "Vlad Mihalcea",
                   "publisher": "Amazon",
                   "price": 44.99
                }
                """
            ));

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

            book.setProperties(JacksonUtil.toJsonNode("""
                {
                   "title": "High-Performance Java Persistence",
                   "author": "Vlad Mihalcea",
                   "publisher": "Amazon",
                   "price": 44.99,
                   "url": "https://www.amazon.com/dp/973022823X/"
                }
                """
            ));
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

    @Test
    public void testUpdateUsingNativeSQL() {

        doInJPA(entityManager -> {
            Book book = entityManager
                .unwrap(Session.class)
                .bySimpleNaturalId(Book.class)
                .load("978-9730228236");

            assertNull(book.getProperties().get("reviews"));

            int updateCount = entityManager.createNativeQuery("""
                UPDATE 
                    book
                SET 
                    properties = jsonb_set(
                        properties,
                        '{reviews}',
                        :reviews
                    )
                WHERE 
                    isbn = :isbn
                """)
            .setParameter("isbn", "978-9730228236")
            .unwrap(org.hibernate.query.Query.class)
            .setParameter(
                "reviews",
                JacksonUtil.toJsonNode("""
                    [
                     	  {
                     		 "date":"2017-11-14",
                     		 "rating":5,
                     		 "review":"Excellent book to understand Java Persistence",
                     		 "reviewer":"Cristiano"
                     	  },
                     	  {
                     		 "date":"2019-01-27",
                     		 "rating":5,
                     		 "review":"The best JPA ORM book out there",
                     		 "reviewer":"T.W"
                     	  },
                     	  {
                     		 "date":"2016-12-24",
                     		 "rating":4,
                     		 "review":"The most informative book",
                     		 "reviewer":"Shaikh"
                     	  }
                     ]
                    """
                ), JsonNodeBinaryType.INSTANCE
            )
            .executeUpdate();

            entityManager.refresh(book);

            JsonNode reviews = book.getProperties().get("reviews");
            assertEquals(3, reviews.size());
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

        @Type(JsonNodeBinaryType.class)
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
