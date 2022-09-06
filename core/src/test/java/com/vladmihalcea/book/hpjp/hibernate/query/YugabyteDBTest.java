package com.vladmihalcea.book.hpjp.hibernate.query;

import com.vladmihalcea.book.hpjp.util.AbstractTest;
import com.vladmihalcea.book.hpjp.util.providers.Database;
import org.junit.Ignore;
import org.junit.Test;

import javax.persistence.*;
import java.sql.Timestamp;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.TemporalAdjusters;
import java.util.List;

import static org.junit.Assert.assertEquals;

/**
 * @author Vlad Mihalcea
 */
public class YugabyteDBTest extends AbstractTest {

    @Override
    protected Class<?>[] entities() {
        return new Class<?>[]{
            Post.class
        };
    }

    @Override
    protected Database database() {
        return Database.YUGABYTEDB;
    }

    private final LocalDate today = LocalDate.now();

    @Override
    public void afterInit() {
        doInJPA(entityManager -> {

            entityManager.persist(
                new Post()
                    .setTitle("High-Performance Java Persistence, Part 1")
                    .setCreatedOn(today.minusDays(2).atStartOfDay())
            );

            entityManager.persist(
                new Post()
                    .setTitle("High-Performance Java Persistence, Part 2")
                    .setCreatedOn(today.minusDays(1).atStartOfDay())
            );

            entityManager.persist(
                new Post()
                    .setTitle("High-Performance Java Persistence, Part 3")
                    .setCreatedOn(today.atStartOfDay())
            );
        });
    }

    @Test
    public void testTimestampRange() {
        doInJPA(entityManager -> {
            List<Post> posts = entityManager.createNativeQuery("""
                SELECT *
                FROM post
                WHERE
                    created_on >= :startTimestamp and 
                    created_on < :endTimestamp
                """, Post.class)
            .setParameter("startTimestamp", today.minusDays(2))
            .setParameter("endTimestamp", today)
            .getResultList();

            assertEquals(2, posts.size());
        });
    }

    @Entity(name = "Post")
    @Table(name = "post")
    public static class Post {

        @Id
        @GeneratedValue
        private Long id;

        private String title;

        @Column(name = "created_on")
        private LocalDateTime createdOn;

        public Long getId() {
            return id;
        }

        public void setId(Long id) {
            this.id = id;
        }

        public String getTitle() {
            return title;
        }

        public Post setTitle(String title) {
            this.title = title;
            return this;
        }

        public LocalDateTime getCreatedOn() {
            return createdOn;
        }

        public Post setCreatedOn(LocalDateTime createdOn) {
            this.createdOn = createdOn;
            return this;
        }
    }

}
