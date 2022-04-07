package com.vladmihalcea.book.hpjp.hibernate.mapping;

import com.vladmihalcea.book.hpjp.util.AbstractTest;
import org.hibernate.annotations.DynamicUpdate;
import org.junit.Test;

import jakarta.persistence.*;
import java.sql.Timestamp;
import java.time.format.DateTimeFormatter;
import java.util.Properties;

/**
 * @author Vlad Mihalcea
 */
public class DynamicUpdateTest extends AbstractTest {

    @Override
    protected Class<?>[] entities() {
        return new Class<?>[] {
            Post.class
        };
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
            Post post1 = new Post();
            post1.setId(1L);
            post1.setTitle("High-Performance Java Persistence");
            entityManager.persist(post1);

            Post post2 = new Post();
            post2.setId(2L);
            post2.setTitle("Java Persistence with Hibernate");
            entityManager.persist(post2);
        });

        doInJPA(entityManager -> {
            Post post1 = entityManager.find(Post.class, 1L);
            post1.setTitle("High-Performance Java Persistence 2nd Edition");

            Post post2 = entityManager.find(Post.class, 2L);
            post2.setLikes(12);
        });
    }

    @Entity(name = "Post")
    @Table(name = "post")
    @DynamicUpdate
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
            return String.format(
                "Post{\n" +
                "  id=%d\n" +
                "  title='%s'\n" +
                "  likes=%d\n" +
                "  creationTimestamp='%s'\n" +
                '}', id, title, likes, getCreationTimestamp()
            );
        }

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

        public long getLikes() {
            return likes;
        }

        public void setLikes(long likes) {
            this.likes = likes;
        }

        public Timestamp getCreatedOn() {
            return createdOn;
        }
    }
}
