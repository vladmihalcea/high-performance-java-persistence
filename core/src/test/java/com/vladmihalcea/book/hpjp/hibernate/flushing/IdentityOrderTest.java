package com.vladmihalcea.book.hpjp.hibernate.flushing;

import com.vladmihalcea.book.hpjp.util.AbstractTest;
import org.junit.Test;

import javax.persistence.*;

/**
 * @author Vlad Mihalcea
 */
public class IdentityOrderTest extends AbstractTest {

    @Override
    protected Class<?>[] entities() {
        return new Class<?>[] {
            Post.class
        };
    }

    private Long postId;


    @Test
    public void tesOperationOrder() {

        EntityManager entityManager = null;
        EntityTransaction txn = null;
        try {
            entityManager = entityManagerFactory().createEntityManager();
            Post post = new Post();
            post.setTitle("High-Performance Java Persistence");
            post.setSlug("high-performance-java-persistence");

            entityManager.persist(post);
            entityManager.getTransaction().begin();
            entityManager.flush();
            entityManager.getTransaction().commit();
        } catch (Throwable e) {
            if ( txn != null && txn.isActive()) txn.rollback();
            throw e;
        } finally {
            if (entityManager != null) {
                entityManager.close();
            }
        }
    }

    @Test
    public void tesOperationOrderWithManualFlush() {
        doInJPA(entityManager -> {
            Post post = entityManager.find(Post.class, postId);
            entityManager.remove(post);
            entityManager.flush();

            Post newPost = new Post();
            newPost.setTitle("High-Performance Java Persistence Book");
            newPost.setSlug("high-performance-java-persistence");
            entityManager.persist(newPost);
        });
    }

    @Entity(name = "Post")
    @Table(name = "post")
    public static class Post {

        @Id
        @GeneratedValue(strategy = GenerationType.IDENTITY)
        private Long id;

        private String title;

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
