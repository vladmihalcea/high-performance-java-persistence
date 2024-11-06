package com.vladmihalcea.hpjp.hibernate.time.offset;

import com.vladmihalcea.hpjp.util.AbstractTest;
import com.vladmihalcea.hpjp.util.providers.Database;
import jakarta.persistence.*;
import org.hibernate.annotations.TimeZoneColumn;
import org.hibernate.annotations.TimeZoneStorage;
import org.hibernate.annotations.TimeZoneStorageType;
import org.junit.Test;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;

import static org.junit.Assert.assertEquals;

/**
 * @author Vlad Mihalcea
 */
public class MySQLOffsetDateTimeTest extends AbstractTest {

    @Override
    protected Class<?>[] entities() {
        return new Class<?>[]{
            Post.class
        };
    }

    @Override
    protected Database database() {
        return Database.MYSQL;
    }

    @Test
    public void test() {
        doInJPA(entityManager -> {
            entityManager.persist(
                new Post()
                    .setId(1L)
                    .setTitle("High-Performance Java Persistence")
                    .setPublishedOn(
                        OffsetDateTime.of(
                            2024, 2, 29,
                            12, 30, 0, 0,
                            ZoneOffset.of("+12:00")
                        )
                    )
            );
        });

        doInJPA(entityManager -> {
            Post post = entityManager.find(
                Post.class, 1L
            );

            assertEquals(
                OffsetDateTime.of(
                    2024, 2, 29,
                    12, 30, 0, 0,
                    ZoneOffset.of("+12:00")
                ),
                post.getPublishedOn()
            );
        });
    }

    @Entity(name = "Post")
    @Table(name = "post")
    public static class Post {

        @Id
        private Long id;

        @Column(length = 100)
        private String title;

        @Column(name = "published_on")
        @TimeZoneStorage(TimeZoneStorageType.COLUMN)
        @TimeZoneColumn(
            name = "published_on_offset"
        )
        private OffsetDateTime publishedOn;

        public Long getId() {
            return id;
        }

        public Post setId(Long id) {
            this.id = id;
            return this;
        }

        public String getTitle() {
            return title;
        }

        public Post setTitle(String title) {
            this.title = title;
            return this;
        }

        public OffsetDateTime getPublishedOn() {
            return publishedOn;
        }

        public Post setPublishedOn(OffsetDateTime publishedOn) {
            this.publishedOn = publishedOn;
            return this;
        }
    }
}
