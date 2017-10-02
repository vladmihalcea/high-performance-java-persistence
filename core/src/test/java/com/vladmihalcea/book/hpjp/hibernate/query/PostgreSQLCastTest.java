package com.vladmihalcea.book.hpjp.hibernate.query;

import com.vladmihalcea.book.hpjp.util.AbstractPostgreSQLIntegrationTest;
import org.hibernate.annotations.GenericGenerator;
import org.junit.Test;

import javax.persistence.*;
import java.sql.Timestamp;
import java.time.DayOfWeek;
import java.time.LocalDateTime;
import java.time.temporal.TemporalAdjusters;
import java.util.List;

import static org.junit.Assert.assertEquals;

/**
 * @author Vlad Mihalcea
 */
public class PostgreSQLCastTest extends AbstractPostgreSQLIntegrationTest {

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
    public void testCastOperator() {
        doInJPA(entityManager -> {
            List<Post> posts = entityManager.createNativeQuery(
                "SELECT * " +
                "FROM post " +
                "WHERE " +
                "   date_part('dow', created_on) = " +
                "   date_part('dow', :datetime::date)", Post.class)
            .setParameter("datetime", Timestamp.valueOf(
                LocalDateTime.now().with(
                    TemporalAdjusters.next(DayOfWeek.MONDAY)))
                )
            .getResultList();
        });
    }

    @Test
    public void testCastFunction() {
        doInJPA(entityManager -> {
            List<Post> posts = entityManager.createNativeQuery(
                "SELECT * " +
                "FROM post " +
                "WHERE " +
                "   date_part('dow', created_on) = " +
                "   date_part('dow', cast(:datetime AS date))", Post.class)
            .setParameter("datetime", Timestamp.valueOf(
                LocalDateTime.now().with(
                    TemporalAdjusters.next(DayOfWeek.MONDAY)))
                )
            .getResultList();

            assertEquals(1, posts.size());
            assertEquals("High-Performance Java Persistence, Part 1", posts.get(0).getTitle());
        });
    }

    @Test
    public void testTimestampFunction() {
        doInJPA(entityManager -> {
            List<Post> posts = entityManager.createNativeQuery(
                "SELECT * " +
                "FROM post " +
                "WHERE " +
                "   date_part('dow', created_on) = " +
                "   date_part('dow', to_timestamp(:datetime, 'YYYY-MM-dd H24:MI:SS'))", Post.class)
            .setParameter("datetime", Timestamp.valueOf(
                LocalDateTime.now().with(TemporalAdjusters.next(DayOfWeek.MONDAY))).toString())
            .getResultList();

            assertEquals(1, posts.size());
            assertEquals("High-Performance Java Persistence, Part 1", posts.get(0).getTitle());
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
