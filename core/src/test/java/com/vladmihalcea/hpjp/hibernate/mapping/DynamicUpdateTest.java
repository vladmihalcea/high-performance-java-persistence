package com.vladmihalcea.hpjp.hibernate.mapping;

import com.vladmihalcea.hpjp.util.AbstractTest;
import org.hibernate.annotations.DynamicUpdate;
import org.junit.Test;

import jakarta.persistence.*;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
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
        });
    }

    @Test
    public void testBatch() {

        doInJPA(entityManager -> {
            for (long i = 1; i <= 5; i++) {
                entityManager.persist(
                    new Post()
                        .setId(i)
                        .setTitle(String.format("Post %s", i))
                );
            }
        });

        doInJPA(entityManager -> {
            List<Post> posts = entityManager.createQuery("""
                select p
                from Post p
                """, Post.class)
            .getResultList();

            for(Post post : posts) {
                if (post.getId() % 2 == 0) {
                    post.setTitle(post.getTitle() + " is great!");
                } else  {
                    post.setLikes(post.getId() * 2);
                }
            }
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
        private LocalDateTime createdOn = LocalDateTime.now();

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

        public LocalDateTime getCreatedOn() {
            return createdOn;
        }
    }
}
