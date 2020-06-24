package com.vladmihalcea.book.hpjp.hibernate.association;

import com.vladmihalcea.book.hpjp.util.AbstractMySQLIntegrationTest;
import com.vladmihalcea.book.hpjp.util.providers.Database;
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
    public void testRemove() {

        doInJPA(entityManager -> {
            Post post = entityManager.createQuery("""
                select p 
                from Post p
                join fetch p.comments
                where p.id = :id
                """, Post.class)
                .setParameter("id", 1L)
                .getSingleResult();

            post.getComments().remove(post.getComments().iterator().next());
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

        public Post addComment(String comment) {
            comments.add(comment);
            return this;
        }
    }
}
