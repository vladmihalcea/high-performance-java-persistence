package com.vladmihalcea.book.hpjp.hibernate.mapping;

import java.sql.Timestamp;
import java.time.format.DateTimeFormatter;
import java.util.Properties;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Transient;

import com.vladmihalcea.book.hpjp.util.providers.Database;
import org.hibernate.annotations.DynamicUpdate;
import org.hibernate.cfg.AvailableSettings;

import org.junit.Test;

import com.vladmihalcea.book.hpjp.util.AbstractTest;

/**
 * @author Vlad Mihalcea
 */
public class DefaultUpdateTest extends AbstractTest {

    @Override
    protected Class<?>[] entities() {
        return new Class<?>[] {
            Post.class
        };
    }

    @Override
    protected Database database() {
        return Database.POSTGRESQL;
    }

    @Override
    protected Properties properties() {
        Properties properties = super.properties();
        properties.put("hibernate.jdbc.batch_size", "5");
        properties.put("hibernate.order_inserts", "true");
        properties.put("hibernate.order_updates", "true");
        properties.put("hibernate.jdbc.batch_versioned_data", "true");
        return properties;
    }

    @Test
    public void test() {
        doInJPA(entityManager -> {
            entityManager.persist(
                new Post()
                    .setId(1L)
                    .setTitle("High-Performance Java Persistence")
            );

            entityManager.persist(
                new Post()
                    .setId(2L)
                    .setTitle("Java Persistence with Hibernate")
            );
        });

        doInJPA(entityManager -> {
            Post post1 = entityManager.find(Post.class, 1L);
            post1.setTitle("High-Performance Java Persistence 2nd Edition");

            Post post2 = entityManager.find(Post.class, 2L);
            post2.setLikes(12);

            entityManager.flush();
        });
    }

    @Entity(name = "Post")
    @Table(name = "post")
    public static class Post {

        @Id
        private Long id;

        private String title;

        private long likes;

        @Column(name = "created_on", nullable = false, updatable = false)
        private Timestamp createdOn;

        @Transient
        private String creationTimestamp;

        public Post() {
            this.createdOn = new Timestamp(System.currentTimeMillis());
        }

        public String getCreationTimestamp() {
            if(creationTimestamp == null) {
                creationTimestamp = DateTimeFormatter.ISO_DATE_TIME.format(
                    createdOn.toLocalDateTime()
                );
            }
            return creationTimestamp;
        }

        @Override
        public String toString() {
            return String.format("""
                Post{
                  id=%d
                  title='%s'
                  likes=%d
                  creationTimestamp='%s'
                }"""
                , id, title, likes, getCreationTimestamp()
            );
        }

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

        public long getLikes() {
            return likes;
        }

        public Post setLikes(long likes) {
            this.likes = likes;
            return this;
        }

        public Timestamp getCreatedOn() {
            return createdOn;
        }
    }
}
