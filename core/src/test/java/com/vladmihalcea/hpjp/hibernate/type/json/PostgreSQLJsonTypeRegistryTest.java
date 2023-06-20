package com.vladmihalcea.hpjp.hibernate.type.json;

import com.fasterxml.jackson.databind.JsonNode;
import com.vladmihalcea.hpjp.util.AbstractPostgreSQLIntegrationTest;
import io.hypersistence.utils.hibernate.type.json.JsonStringType;
import io.hypersistence.utils.hibernate.type.json.JsonType;
import jakarta.persistence.*;
import org.hibernate.annotations.NaturalId;
import org.hibernate.annotations.Type;
import org.hibernate.jpa.boot.spi.TypeContributorList;
import org.hibernate.query.NativeQuery;
import org.junit.Test;

import java.util.Collections;
import java.util.Properties;

import static org.hibernate.jpa.boot.internal.EntityManagerFactoryBuilderImpl.TYPE_CONTRIBUTORS;
import static org.junit.Assert.*;

/**
 * @author Vlad Mihalcea
 */
public class PostgreSQLJsonTypeRegistryTest extends AbstractPostgreSQLIntegrationTest {

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
                    .setProperties("""
                        {
                           "title": "High-Performance Java Persistence",
                           "author": "Vlad Mihalcea",
                           "publisher": "Amazon",
                           "price": 44.99
                        }
                        """
                    )
            );
        });
    }

    @Override
    protected void additionalProperties(Properties properties) {
        properties.put(TYPE_CONTRIBUTORS,
            (TypeContributorList) () -> Collections.singletonList(
                (typeContributions, serviceRegistry) -> {
                    typeContributions.contributeType(new JsonStringType(JsonNode.class));
                }
            )
        );
    }

    @Test
    public void test() {
        doInJPA(entityManager -> {
            JsonNode properties = (JsonNode) entityManager.createNativeQuery("""
                SELECT
                  properties AS properties
                FROM book
                WHERE
                  isbn = :isbn
                """)
            .setParameter("isbn", "978-9730228236")
            .unwrap(NativeQuery.class)
            .addScalar("properties", JsonNode.class)
            .getSingleResult();

            assertEquals("High-Performance Java Persistence", properties.get("title").asText());
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

        @Type(JsonType.class)
        @Column(columnDefinition = "jsonb")
        private String properties;

        public String getIsbn() {
            return isbn;
        }

        public Book setIsbn(String isbn) {
            this.isbn = isbn;
            return this;
        }

        public String getProperties() {
            return properties;
        }

        public Book setProperties(String properties) {
            this.properties = properties;
            return this;
        }
    }
}