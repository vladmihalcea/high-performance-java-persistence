package com.vladmihalcea.book.hpjp.hibernate.association;

import com.vladmihalcea.book.hpjp.util.AbstractMySQLIntegrationTest;
import org.junit.Test;

import javax.persistence.ElementCollection;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Table;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * @author Vlad Mihalcea
 */
public class ElementCollectionSetTest extends AbstractMySQLIntegrationTest {

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

            post.getComments().add("My first review");
            post.getComments().add("My second review");
            post.getComments().add("My third review");

            entityManager.persist(post);
        });

        doInJPA(entityManager -> {
            Post post = entityManager.find(Post.class, 1L);

            LOGGER.info("Remove tail");
            post.getComments().remove("My third review");
        });

        doInJPA(entityManager -> {
            Post post = entityManager.find(Post.class, 1L);

            LOGGER.info("Remove head");
            post.getComments().remove("My first review");
        });
    }

    @Entity(name = "Post")
    @Table(name = "post")
    public static class Post {

        @Id
        private Long id;

        private String title;

        @ElementCollection
        private Set<String> comments = new HashSet<>();

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

        public Set<String> getComments() {
            return comments;
        }
    }
}
