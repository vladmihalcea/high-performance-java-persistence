package com.vladmihalcea.hpjp.hibernate.query.time;

import com.vladmihalcea.hpjp.util.AbstractPostgreSQLIntegrationTest;
import jakarta.persistence.*;
import org.junit.Test;

import java.time.DayOfWeek;
import java.time.LocalDateTime;
import java.time.Year;
import java.time.temporal.TemporalAdjusters;
import java.util.List;

import static org.junit.Assert.assertEquals;

/**
 * @author Vlad Mihalcea
 */
public class ExtractTest extends AbstractPostgreSQLIntegrationTest {

    @Override
    protected Class<?>[] entities() {
        return new Class<?>[]{
            Post.class
        };
    }

    @Override
    public void afterInit() {
        doInJPA(entityManager -> {
            Post part1 = new Post();
            part1.setTitle("High-Performance Java Persistence, Part 1");
            part1.setCreatedOn(
                LocalDateTime.now().with(TemporalAdjusters.previous(DayOfWeek.MONDAY))
            );
            entityManager.persist(part1);

            Post part2 = new Post();
            part2.setTitle("High-Performance Java Persistence, Part 2");
            part2.setCreatedOn(
                LocalDateTime.now().with(TemporalAdjusters.previous(DayOfWeek.TUESDAY))
            );
            entityManager.persist(part2);

            Post part3 = new Post();
            part3.setTitle("High-Performance Java Persistence, Part 3");
            part3.setCreatedOn(
                LocalDateTime.now().with(TemporalAdjusters.previous(DayOfWeek.THURSDAY))
            );
            entityManager.persist(part3);
        });
    }

    @Test
    public void test() {
        doInJPA(entityManager -> {
            List<Post> posts = entityManager.createQuery("""
                select p
                from Post p
                where EXTRACT(YEAR FROM createdOn) = :year
                """, Post.class)
            .setParameter("year", Year.now().getValue())
            .getResultList();

            assertEquals(3, posts.size());
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

        public void setTitle(String title) {
            this.title = title;
        }

        public LocalDateTime getCreatedOn() {
            return createdOn;
        }

        public void setCreatedOn(LocalDateTime createdOn) {
            this.createdOn = createdOn;
        }
    }

}
