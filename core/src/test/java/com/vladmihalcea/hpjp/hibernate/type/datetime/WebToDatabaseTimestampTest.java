package com.vladmihalcea.hpjp.hibernate.type.datetime;

import com.vladmihalcea.hpjp.util.AbstractTest;
import com.vladmihalcea.hpjp.util.providers.Database;
import jakarta.persistence.*;
import org.junit.Test;

import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;

/**
 * @author Vlad Mihalcea
 */
public class WebToDatabaseTimestampTest extends AbstractTest {

    @Override
    protected Class<?>[] entities() {
        return new Class<?>[] {
            Book.class
        };
    }
    @Override
    protected Database database() {
        return Database.POSTGRESQL;
    }

    @Test
    public void test() {
        doInJPA(entityManager -> {
            Book book = new Book()
                .setId(1)
                .setTitle("High-Performance Java Persistence, 1st edition")
                .setAuthor("Vlad Mihalcea")
                .setPublishedOn(
                    OffsetDateTime.parse("2016-10-12T12:23:45.0+02:00")
                        .withOffsetSameInstant(ZoneOffset.UTC)
                        .toLocalDateTime()
                );

            entityManager.persist(book);
        });

        doInJPA(entityManager -> {
            Book book = entityManager.find(Book.class, 1);

            LOGGER.info(
                "The first edition of High-Performance Java Persistence was published on {}",
                book.getPublishedOn()
            );
        });
    }

    @Entity(name = "Book")
    @Table(name = "book")
    public static class Book {

        @Id
        private Integer id;

        @Column(length = 100)
        private String title;

        @Column(name = "author", length = 50)
        private String author;

        @Column(name = "published_on", columnDefinition = "timestamp")
        private LocalDateTime publishedOn;

        public Integer getId() {
            return id;
        }

        public Book setId(Integer id) {
            this.id = id;
            return this;
        }

        public String getTitle() {
            return title;
        }

        public Book setTitle(String title) {
            this.title = title;
            return this;
        }

        public LocalDateTime getPublishedOn() {
            return publishedOn;
        }

        public Book setPublishedOn(LocalDateTime publishedOn) {
            this.publishedOn = publishedOn;
            return this;
        }

        public String getAuthor() {
            return author;
        }

        public Book setAuthor(String author) {
            this.author = author;
            return this;
        }
    }
}
