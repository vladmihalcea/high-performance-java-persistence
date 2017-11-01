package com.vladmihalcea.book.hpjp.hibernate.type.json;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.Table;

import com.vladmihalcea.book.hpjp.util.providers.DataSourceProvider;
import com.vladmihalcea.book.hpjp.util.providers.PostgreSQLDataSourceProvider;
import org.hibernate.Session;
import org.hibernate.annotations.NaturalId;
import org.hibernate.annotations.Type;
import org.hibernate.annotations.TypeDef;

import org.hibernate.boot.model.TypeContributions;
import org.hibernate.boot.model.TypeContributor;
import org.hibernate.dialect.PostgreSQL95Dialect;
import org.hibernate.jpa.boot.spi.TypeContributorList;
import org.hibernate.service.ServiceRegistry;
import org.junit.Test;

import com.fasterxml.jackson.databind.JsonNode;
import com.vladmihalcea.book.hpjp.util.AbstractPostgreSQLIntegrationTest;

import java.sql.Types;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Properties;

/**
 * @author Vlad Mihalcea
 */
public class PostgreSQLJsonNodeBinaryTypeTest extends AbstractPostgreSQLIntegrationTest {

    @Override
    protected Class<?>[] entities() {
        return new Class<?>[] {
            Book.class
        };
    }

    @Override
    protected void additionalProperties(Properties properties) {
        properties.put( "hibernate.type_contributors", (TypeContributorList) () -> Arrays.asList(
            (TypeContributor) (typeContributions, serviceRegistry) ->
                typeContributions.contributeType(new JsonNodeBinaryType())
        ) );
    }

    protected DataSourceProvider dataSourceProvider() {
        return new PostgreSQLDataSourceProvider() {
            @Override
            public String hibernateDialect() {
                return PostgreSQL95JsonBDialect.class.getName();
            }
        };
    }

    public static class PostgreSQL95JsonBDialect extends PostgreSQL95Dialect {

        public PostgreSQL95JsonBDialect() {
            super();
            this.registerHibernateType( Types.OTHER, "jsonb-node" );
        }
    }

    @Test
    public void test() {

        doInJPA(entityManager -> {

            Book book = new Book();
            book.setIsbn( "978-9730228236" );
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

            entityManager.persist( book );
        });

        doInJPA(entityManager -> {
            Session session = entityManager.unwrap( Session.class );
            Book book = session
                .bySimpleNaturalId( Book.class )
                .load( "978-9730228236" );

            LOGGER.info( "Book details: {}", book.getProperties() );

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

        doInJPA(entityManager -> {
            List<?> properties = entityManager.createNativeQuery(
                "select properties from book")
            .getResultList();

            properties.size();
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

        @Type( type = "jsonb-node" )
        @Column(columnDefinition = "jsonb")
        private JsonNode properties;

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
