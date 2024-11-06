package com.vladmihalcea.hpjp.hibernate.flushing;

import com.vladmihalcea.hpjp.util.AbstractTest;
import org.junit.Test;

import jakarta.persistence.*;

/**
 * @author Vlad Mihalcea
 */
public class PersistSequenceTest extends AbstractTest {

    @Override
    protected Class<?>[] entities() {
        return new Class<?>[] {
            Post.class
        };
    }

    @Test
    public void testId() {

        doInJPA(entityManager -> {
            Post post = new Post();
            post.setTitle("High-Performance Java Persistence");

            entityManager.persist(post);
            LOGGER.info("The post entity identifier is {}", post.getId());

            LOGGER.info("Flush Persistence Context");
            entityManager.flush();
        });
    }

    @Test
    public void testMerge() {

        Post post = doInJPA(entityManager -> {
            Post _post = new Post();
            _post.setTitle("High-Performance Java Persistence");

            entityManager.persist(_post);
            return _post;
        });

        post.setTitle("High-Performance Java Persistence Rocks!");

        doInJPA(entityManager -> {
            LOGGER.info("Merging the Post entity");
            Post post_ = entityManager.merge(post);
        });
    }

    @Test
    public void testRedundantMerge() {

        doInJPA(entityManager -> {
            Post post = new Post();
            post.setTitle("High-Performance Java Persistence");

            entityManager.persist(post);
            entityManager.merge(post);
        });
    }

    @Entity(name = "Post") @Table(name = "post")
    public static class Post {

        @Id
        @GeneratedValue
        private Long id;

        private String title;

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
    }
}
