package com.vladmihalcea.hpjp.hibernate.concurrency.version;

import com.vladmihalcea.hpjp.util.AbstractTest;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import org.junit.Test;

import java.time.LocalDateTime;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

/**
 * @author Vlad Mihalcea
 */
public class TimestampVersionTest extends AbstractTest {

    @Override
    protected Class<?>[] entities() {
        return new Class<?>[]{
            Post.class
        };
    }

    @Test
    public void testOptimisticLocking() {

        doInJPA(entityManager -> {
            Post post = new Post();
            post.setId(1L);
            post.setTitle("High-Performance Java Persistence");
            entityManager.persist(post);

            entityManager.flush();
            post.setTitle("High-Performance Hibernate");
        });

        doInJPA(entityManager -> {
            Post post = entityManager.find(Post.class, 1L);
            assertNotNull(post.getVersion());
        });
    }

    @Entity(name = "Post")
    @Table(name = "post")
    public static class Post {

        @Id
        private Long id;

        private String title;

        @Version
        private LocalDateTime version;

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

        public LocalDateTime getVersion() {
            return version;
        }

        public void setVersion(LocalDateTime version) {
            this.version = version;
        }
    }

}
