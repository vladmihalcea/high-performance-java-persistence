package com.vladmihalcea.book.hpjp.hibernate.flushing;

import com.vladmihalcea.book.hpjp.util.AbstractTest;
import org.hibernate.Session;
import org.hibernate.annotations.NaturalId;
import org.junit.Ignore;
import org.junit.Test;

import javax.persistence.*;

/**
 * @author Vlad Mihalcea
 */
public class FlushOrderTest extends AbstractTest {

    @Override
    protected Class<?>[] entities() {
        return new Class<?>[] {
            Post.class
        };
    }

    @Override
    public void init() {
        super.init();
        doInJPA(entityManager -> {
            Post post = new Post();
            post.setId(1L);
            post.setTitle("High-Performance Java Persistence");
            post.setSlug("high-performance-java-persistence");

            entityManager.persist(post);
        });
    }

    @Test
    @Ignore
    public void testOperationOrder() {
        doInJPA(entityManager -> {
            Post post = entityManager.find(Post.class, 1L);
            entityManager.remove(post);

            Post newPost = new Post();
            newPost.setId(2L);
            newPost.setTitle("High-Performance Java Persistence Book");
            newPost.setSlug("high-performance-java-persistence");
            entityManager.persist(newPost);
        });
    }

    @Test
    public void testOperationOrderWithManualFlush() {
        doInJPA(entityManager -> {
            Post post = entityManager.find(Post.class, 1L);
            entityManager.remove(post);

            entityManager.flush();

            Post newPost = new Post();
            newPost.setId(2L);
            newPost.setTitle("High-Performance Java Persistence Book");
            newPost.setSlug("high-performance-java-persistence");
            entityManager.persist(newPost);
        });
    }

    @Test
    public void testUpdate() {
        doInJPA(entityManager -> {
            Post post = entityManager.unwrap(Session.class)
            .bySimpleNaturalId(Post.class)
            .load("high-performance-java-persistence");

            post.setTitle("High-Performance Java Persistence Book");
        });
    }

    @Entity(name = "Post")
    @Table(
        name = "post",
        uniqueConstraints = @UniqueConstraint(
            name = "slug_uq",
            columnNames = "slug"
        )
    )
    public static class Post {

        @Id
        private Long id;

        private String title;

        @NaturalId
        private String slug;

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

        public String getSlug() {
            return slug;
        }

        public void setSlug(String slug) {
            this.slug = slug;
        }
    }
}
