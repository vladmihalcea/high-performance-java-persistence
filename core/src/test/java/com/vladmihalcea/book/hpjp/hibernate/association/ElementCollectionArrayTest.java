package com.vladmihalcea.book.hpjp.hibernate.association;

import com.vladmihalcea.book.hpjp.util.AbstractMySQLIntegrationTest;
import org.junit.Test;

import jakarta.persistence.*;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * @author Vlad Mihalcea
 */
public class ElementCollectionArrayTest extends AbstractMySQLIntegrationTest {

    @Override
    protected Class<?>[] entities() {
        return new Class<?>[] {
            Post.class
        };
    }

    @Test
    public void testLifecycle() {
        doInJPA(entityManager -> {
            Post post = new Post()
                .setId(1L)
                .setTitle("High-Performance Java Persistence");

            post.setComments(new String[] {
                    "My first review",
                    "My second review",
                    "My third review",
            });

            entityManager.persist(post);
        });

        doInJPA(entityManager -> {
            Post post = entityManager.find(Post.class, 1L);

            LOGGER.info("Remove tail");
            post.setComments(Arrays.copyOf(post.getComments(), 2));
        });

        doInJPA(entityManager -> {
            Post post = entityManager.find(Post.class, 1L);

            LOGGER.info("Remove head");
            post.setComments(Arrays.copyOfRange(post.getComments(), 1, 2));
        });
    }

    @Entity(name = "Post")
    @Table(name = "post")
    public static class Post {

        @Id
        private Long id;

        private String title;

        @ElementCollection
        @OrderColumn(name = "position")
        private String[] comments;

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

        public String[] getComments() {
            return comments;
        }

        public void setComments(String[] comments) {
            this.comments = comments;
        }
    }
}
