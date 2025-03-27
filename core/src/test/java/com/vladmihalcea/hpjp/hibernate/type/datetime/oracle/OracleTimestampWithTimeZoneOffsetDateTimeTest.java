package com.vladmihalcea.hpjp.hibernate.type.datetime.oracle;

import com.vladmihalcea.hpjp.util.AbstractTest;
import com.vladmihalcea.hpjp.util.providers.Database;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import org.hibernate.Session;
import org.hibernate.annotations.DynamicInsert;
import org.hibernate.annotations.DynamicUpdate;
import org.hibernate.annotations.NaturalId;
import org.junit.Test;

import java.sql.ResultSet;
import java.sql.Statement;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;

import static org.junit.Assert.assertEquals;

/**
 * @author Vlad Mihalcea
 */
public class OracleTimestampWithTimeZoneOffsetDateTimeTest extends AbstractTest {

    @Override
    protected Class<?>[] entities() {
        return new Class<?>[] {
            Book.class
        };
    }

    @Override
    protected Database database() {
        return Database.ORACLE;
    }

    @Test
    public void testPersist() {
        doInJPA(entityManager -> {
            entityManager.persist(
                new Book()
                    .setId(1)
                    .setIsbn("978-9730228236")
                    .setTitle("High-Performance Java Persistence")
                    .setAuthor("Vlad Mihalcea")
                    .setPublishedOn(
                        OffsetDateTime.of(
                            2016, 10, 12, 7, 30, 45, 0,
                            ZoneOffset.of("+02:00")
                        )
                    )
            );
        });

        doInJPA(entityManager -> {
            entityManager.unwrap(Session.class).doWork(connection -> {
                try(Statement statement = connection.createStatement()) {
                    ResultSet resultSet = statement.executeQuery("""
                        SELECT title, published_on 
                        FROM book
                        WHERE id = 1
                        """);

                    LOGGER.info("{}{}", System.lineSeparator(), resultSetToString(resultSet));
                }
            });
        });

        doInJPA(entityManager -> {
            Book book = entityManager.find(Book.class, 1);
            assertEquals(
                book.getPublishedOn(),
                OffsetDateTime.of(
                    2016, 10, 12, 7, 30, 45, 0,
                    ZoneOffset.of("+02:00")
                )
            );

            book.setUpdatedOn(
                OffsetDateTime.of(
                    2024, 7, 18, 10, 45, 0, 0,
                    ZoneOffset.of("+01:00")
                )
            );
        });

        doInJPA(entityManager -> {
            entityManager.unwrap(Session.class).doWork(connection -> {
                try(Statement statement = connection.createStatement()) {
                    ResultSet resultSet = statement.executeQuery("""
                        SELECT title, updated_on 
                        FROM book
                        WHERE id = 1
                        """);

                    LOGGER.info("{}{}", System.lineSeparator(), resultSetToString(resultSet));
                }
            });
        });

        doInJPA(entityManager -> {
            Book book = entityManager.find(Book.class, 1);
            assertEquals(
                book.getUpdatedOn(),
                OffsetDateTime.of(
                    2024, 7, 18, 10, 45, 0, 0,
                    ZoneOffset.of("+01:00")
                )
            );
        });
    }

    @Entity(name = "Book")
    @Table(name = "book")
    @DynamicInsert @DynamicUpdate
    public static class Book {

        @Id
        private Integer id;

        @NaturalId
        @Column(length = 15)
        private String isbn;

        @Column(length = 50)
        private String title;

        @Column(length = 50)
        private String author;

        @Column(name = "published_on", columnDefinition = "TIMESTAMP WITH TIME ZONE")
        private OffsetDateTime publishedOn;

        @Column(name = "updated_on", columnDefinition = "TIMESTAMP WITH TIME ZONE")
        private OffsetDateTime updatedOn;

        public Integer getId() {
            return id;
        }

        public Book setId(Integer id) {
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

        public String getTitle() {
            return title;
        }

        public Book setTitle(String title) {
            this.title = title;
            return this;
        }

        public String getAuthor() {
            return author;
        }

        public Book setAuthor(String author) {
            this.author = author;
            return this;
        }

        public OffsetDateTime getPublishedOn() {
            return publishedOn;
        }

        public Book setPublishedOn(OffsetDateTime publishedOn) {
            this.publishedOn = publishedOn;
            return this;
        }

        public OffsetDateTime getUpdatedOn() {
            return updatedOn;
        }

        public Book setUpdatedOn(OffsetDateTime timestampProperty) {
            this.updatedOn = timestampProperty;
            return this;
        }
    }
}
