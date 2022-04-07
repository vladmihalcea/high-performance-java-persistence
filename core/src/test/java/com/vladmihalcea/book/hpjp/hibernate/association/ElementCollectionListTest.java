package com.vladmihalcea.book.hpjp.hibernate.association;

import com.vladmihalcea.book.hpjp.util.AbstractTest;
import com.vladmihalcea.book.hpjp.util.providers.Database;
import org.junit.Test;

import jakarta.persistence.*;
import java.util.ArrayList;
import java.util.List;

/**
 * @author Vlad Mihalcea
 */
public class ElementCollectionListTest extends AbstractTest {

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
    protected void afterInit() {
        doInJPA(entityManager -> {
            entityManager.persist(
                new Post()
                    .setId(1L)
                    .setTitle("High-Performance Java Persistence")
                    .addComment("Best book on JPA and Hibernate!")
                    .addComment("A must-read for every Java developer!")
                    .addComment("A great reference book")
            );
        });
    }

    @Test
    public void testRemoveTail() {

        doInJPA(entityManager -> {
            Post post = entityManager.find(Post.class, 1L);

            List<String> comments = post.getComments();
            comments.remove(comments.size() - 1);
        });
    }
    
    @Test
    public void testFetchAndRemoveTail() {

        doInJPA(entityManager -> {
            Post post = entityManager.createQuery("""
                select p 
                from Post p
                join fetch p.comments
                where p.id = :id
                """, Post.class)
                .setParameter("id", 1L)
                .getSingleResult();

            post.getComments().remove(post.getComments().size() - 1);
        });
    }

    @Test
    public void testFetchAndRemoveHead() {

        doInJPA(entityManager -> {
            Post post = entityManager.createQuery("""
                select p
                from Post p
                join fetch p.comments
                where p.id = :id
                """, Post.class)
                .setParameter("id", 1L)
                .getSingleResult();

            post.getComments().remove(0);
        });
    }

    @Entity(name = "Post")
    @Table(name = "post")
    public static class Post {

        @Id
        private Long id;

        private String title;

        @ElementCollection
        @JoinTable(name = "post_comments")
        private List<String> comments = new ArrayList<>();

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

        public List<String> getComments() {
            return comments;
        }

        public Post addComment(String comment) {
            comments.add(comment);
            return this;
        }
    }
}
