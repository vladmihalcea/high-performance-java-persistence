package com.vladmihalcea.book.hpjp.hibernate.cache;

import com.vladmihalcea.book.hpjp.util.AbstractTest;
import org.hibernate.annotations.Cache;
import org.hibernate.annotations.CacheConcurrencyStrategy;
import org.junit.Before;
import org.junit.Test;

import jakarta.persistence.*;
import java.util.Date;
import java.util.Properties;

import static org.junit.Assert.assertNotNull;

/**
 * @author Vlad Mihalcea
 */
public class EntityCacheEntryLoadedStateTest extends AbstractTest {

    @Override
    protected Class<?>[] entities() {
        return new Class<?>[] {
            Post.class,
            PostDetails.class,
            PostComment.class
        };
    }

    @Override
    protected void additionalProperties(Properties properties) {
        properties.put("hibernate.cache.region.factory_class", "jcache");
        properties.put("hibernate.generate_statistics", Boolean.TRUE.toString());
    }

    @Override
    protected void afterInit() {
        doInJPA(entityManager -> {
            entityManager.persist(
                new Post()
                    .setId(1L)
                    .setTitle("High-Performance Java Persistence")
            );
        });


        doInJPA(entityManager -> {
            Post post = entityManager.getReference(Post.class,1L);

            entityManager.persist(
                new PostDetails()
                    .setCreatedBy("Vlad Mihalcea")
                    .setCreatedOn(new Date())
                    .setPost(post)
            );

            entityManager.persist(
                new PostComment()
                    .setId(1L)
                    .setReview("Part one - JDBC and Database Essentials")
                    .setPost(post)
            );

            entityManager.persist(
                new PostComment()
                    .setId(2L)
                    .setReview("Part one - JPA and Hibernate")
                    .setPost(post)
            );
        });
    }

    @Test
    public void testEntityLoad() {

        doInJPA(entityManager -> {
            Post post = entityManager.find(Post.class, 1L);
            assertNotNull(post);
            PostDetails details = entityManager.find(PostDetails.class, 1L);
            assertNotNull(details);
            PostComment comment = entityManager.find(PostComment.class, 1L);
            assertNotNull(comment);
        });

        printCacheRegionStatistics(PostComment.class.getName());
        printCacheRegionStatistics(PostDetails.class.getName());
        printCacheRegionStatistics(Post.class.getName());

        doInJPA(entityManager -> {
            LOGGER.info("Load from cache");
            Post post = entityManager.find(Post.class, 1L);
            assertNotNull(post);
            PostDetails details = entityManager.find(PostDetails.class, 1L);
            assertNotNull(details);
            PostComment comment = entityManager.find(PostComment.class, 1L);
            assertNotNull(comment);
        });

        printCacheRegionStatistics(Post.class.getName());
    }

    @Entity(name = "Post")
    @Table(name = "post")
    @Cache(usage = CacheConcurrencyStrategy.READ_WRITE)
    public static class Post {

        @Id
        private Long id;

        private String title;

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
    }

    @Entity(name = "PostDetails")
    @Cache(usage = CacheConcurrencyStrategy.READ_WRITE)
    public static class PostDetails {

        @Id
        private Long id;

        @Column(name = "created_on")
        private Date createdOn;

        @Column(name = "created_by")
        private String createdBy;

        @OneToOne(fetch = FetchType.LAZY)
        @MapsId
        private Post post;

        public Long getId() {
            return id;
        }

        public PostDetails setId(Long id) {
            this.id = id;
            return this;
        }

        public Post getPost() {
            return post;
        }

        public PostDetails setPost(Post post) {
            this.post = post;
            return this;
        }

        public Date getCreatedOn() {
            return createdOn;
        }

        public PostDetails setCreatedOn(Date createdOn) {
            this.createdOn = createdOn;
            return this;
        }

        public String getCreatedBy() {
            return createdBy;
        }

        public PostDetails setCreatedBy(String createdBy) {
            this.createdBy = createdBy;
            return this;
        }
    }

    @Entity(name = "PostComment")
    @Table(name = "post_comment")
    @Cache(usage = CacheConcurrencyStrategy.READ_WRITE)
    public static class PostComment {

        @Id
        private Long id;

        @ManyToOne(fetch = FetchType.LAZY)
        private Post post;

        private String review;

        public Long getId() {
            return id;
        }

        public PostComment setId(Long id) {
            this.id = id;
            return this;
        }

        public Post getPost() {
            return post;
        }

        public PostComment setPost(Post post) {
            this.post = post;
            return this;
        }

        public String getReview() {
            return review;
        }

        public PostComment setReview(String review) {
            this.review = review;
            return this;
        }
    }
}
