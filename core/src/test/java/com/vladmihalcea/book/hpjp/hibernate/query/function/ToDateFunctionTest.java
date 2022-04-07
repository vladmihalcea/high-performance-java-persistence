package com.vladmihalcea.book.hpjp.hibernate.query.function;

import com.vladmihalcea.book.hpjp.util.AbstractMySQLIntegrationTest;
import com.vladmihalcea.book.hpjp.util.AbstractPostgreSQLIntegrationTest;
import org.hibernate.boot.MetadataBuilder;
import org.hibernate.boot.spi.MetadataBuilderContributor;
import org.hibernate.dialect.function.StandardSQLFunction;
import org.hibernate.type.StandardBasicTypes;
import org.junit.Test;

import jakarta.persistence.*;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.*;

import static org.junit.Assert.assertEquals;

/**
 * @author Vlad Mihalcea
 */
public class ToDateFunctionTest extends AbstractPostgreSQLIntegrationTest {

    @Override
    protected Class<?>[] entities() {
        return new Class<?>[]{
                Post.class,
        };
    }

    @Override
    public void afterInit() {
        doInJPA(entityManager -> {

            Post post = new Post();
            post.setId(1L);
            post.setTitle("High-Performance Java Persistence");
            post.setCreatedOn(LocalDate.of(2018, 11, 23).toString());

            entityManager.persist(post);
        });
    }

    @Test
    public void test() {
        doInJPA(entityManager -> {
            Tuple tuple = entityManager
            .createQuery(
                "select p.title as title, TO_DATE(p.createdOn, 'YYYY-MM-dd') " +
                "from Post p " +
                "where p.id = :postId", Tuple.class)
            .setParameter("postId", 1L)
            .getSingleResult();

            assertEquals("High-Performance Java Persistence", tuple.get("title"));
        });
    }

    @Entity(name = "Post")
    @Table(name = "post")
    public static class Post {

        @Id
        private Long id;

        private String title;

        @Column(name = "created_on")
        private String createdOn;

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

        public String getCreatedOn() {
            return createdOn;
        }

        public void setCreatedOn(String createdOn) {
            this.createdOn = createdOn;
        }
    }

}
