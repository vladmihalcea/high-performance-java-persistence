package com.vladmihalcea.book.hpjp.hibernate.flushing;

import com.vladmihalcea.book.hpjp.util.AbstractTest;
import org.junit.Test;

import javax.persistence.*;

/**
 * <code>OrderTest</code> - Order Test
 *
 * @author Vlad Mihalcea
 */
public class OrderTest extends AbstractTest {

    @Override
    protected Class<?>[] entities() {
        return new Class<?>[] {
            Post.class
        };
    }

    private Long postId;

    @Override
    public void init() {
        super.init();
        postId = doInJPA(entityManager -> {
            Post post = new Post();
            post.setTitle("High-Performance Java Persistence");
            post.setSlug("high-Performance-java-persistence");

            entityManager.persist(post);
            entityManager.flush();
            return post.getId();
        });
    }

    @Test(expected = PersistenceException.class)
    public void tesOperationOrder() {
        doInJPA(entityManager -> {
            Post post = entityManager.find(Post.class, postId);
            entityManager.remove(post);

            Post newPost = new Post();
            newPost.setTitle("High-Performance Java Persistence Book");
            newPost.setSlug("high-Performance-java-persistence");
            entityManager.persist(newPost);
        });
    }

    @Test
    public void tesOperationOrderWithManualFlush() {
        doInJPA(entityManager -> {
            Post post = entityManager.find(Post.class, postId);
            entityManager.remove(post);
            entityManager.flush();

            Post newPost = new Post();
            newPost.setTitle("High-Performance Java Persistence Book");
            newPost.setSlug("high-Performance-java-persistence");
            entityManager.persist(newPost);
        });
    }

    @Entity(name = "Post") @Table(name = "post")
    public static class Post {

        @Id
        @GeneratedValue
        private Long id;

        private String title;

        @Column(unique = true)
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
