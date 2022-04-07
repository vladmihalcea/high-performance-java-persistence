package com.vladmihalcea.book.hpjp.hibernate.concurrency;

import com.vladmihalcea.book.hpjp.util.AbstractTest;
import org.hibernate.Session;
import org.hibernate.annotations.DynamicUpdate;
import org.hibernate.annotations.OptimisticLockType;
import org.hibernate.annotations.OptimisticLocking;
import org.hibernate.annotations.SelectBeforeUpdate;
import org.junit.Test;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/**
 * OptimisticLockingOneRootDirtyVersioningTest - Test to check optimistic checking on a single entity being updated by many threads
 * using the dirty properties instead of a synthetic version column
 *
 * @author Vlad Mihalcea
 */
public class OptimisticLockingOneRootDirtyVersioningSelectBeforeUpdateTest extends AbstractTest {

    @Override
    protected Class<?>[] entities() {
        return new Class<?>[]{
                Post.class
        };
    }

    @Test
    public void testVersionlessOptimisticLockingWhenReattaching() {

        doInJPA(entityManager -> {
            Post post = new Post();
            post.setId(1L);
            post.setTitle("JDBC");
            entityManager.persist(post);
            return post;
        });

        Post detachedPost = doInJPA(entityManager -> {
            LOGGER.info("Alice loads the Post entity");
            return entityManager.find(Post.class, 1L);
        });

        executeSync(() -> {
            doInJPA(entityManager -> {
                LOGGER.info("Bob loads the Post entity and modifies it");
                Post post = entityManager.find(Post.class, 1L);
                post.setTitle("Hibernate");
            });
        });

        doInJPA(entityManager -> {
            LOGGER.info("Alice updates the Post entity");
            detachedPost.setTitle("JPA");
            entityManager.unwrap(Session.class).update(detachedPost);
        });
    }
    
    @Entity(name = "Post") @Table(name = "post")
    @OptimisticLocking(type = OptimisticLockType.DIRTY)
    @SelectBeforeUpdate
    @DynamicUpdate
    public static class Post {

        @Id
        private Long id;

        private String title;

        private long views;

        private int likes;

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

        public long getViews() {
            return views;
        }

        public int getLikes() {
            return likes;
        }

        public int incrementLikes() {
            return ++likes;
        }

        public void setViews(long views) {
            this.views = views;
        }
    }
}
