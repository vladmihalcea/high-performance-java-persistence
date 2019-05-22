package com.vladmihalcea.book.hpjp.hibernate.cache;

import com.vladmihalcea.book.hpjp.util.AbstractTest;
import org.hibernate.annotations.CacheConcurrencyStrategy;
import org.junit.Before;
import org.junit.Test;

import java.util.Date;
import java.util.Properties;

import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.OneToOne;
import javax.persistence.Table;

import static org.junit.Assert.assertNotNull;

public class BidirectionalOneToOneCacheTest extends AbstractTest {

    @Override
    protected Class<?>[] entities() {
        return new Class<?>[]{
                Post.class,
                PostDetails.class,
        };
    }

    @Before
    public void init() {
        super.init();
        doInJPA(entityManager -> {
            Post post = new Post("First post");
            PostDetails details = new PostDetails("John Doe");
            post.setDetails(details);
            details.setPost(post);
            entityManager.persist(post);
        });
    }

    @Override
    protected Properties properties() {
        Properties properties = super.properties();
        properties.put("hibernate.cache.use_second_level_cache", Boolean.TRUE.toString());
        properties.put("hibernate.cache.region.factory_class", "ehcache");
        properties.put("hibernate.generate_statistics", Boolean.TRUE.toString());
        return properties;
    }

    @Test
    public void testEntityLoad() {

        doInJPA(entityManager -> {
            LOGGER.info("First access");
            Post post = entityManager.find(Post.class, 1L);
            assertNotNull(post);
            assertNotNull(post.getDetails());
        });

        printCacheRegionStatistics(Post.class.getName());
        printCacheRegionStatistics(PostDetails.class.getName());

        doInJPA(entityManager -> {
            LOGGER.info("Second access");
            Post post = entityManager.find(Post.class, 1L);
            assertNotNull(post);
            assertNotNull(post.getDetails());
        });

        printCacheRegionStatistics(Post.class.getName());
        printCacheRegionStatistics(PostDetails.class.getName());

        doInJPA(entityManager -> {
            LOGGER.info("Only find PostDetails");
            PostDetails details = entityManager.find(PostDetails.class, 2L);
            assertNotNull(details);
        });

        printCacheRegionStatistics(PostDetails.class.getName());
    }

    @Entity(name = "Post")
    @Table(name = "post")
    @org.hibernate.annotations.Cache(usage = CacheConcurrencyStrategy.READ_WRITE)
    public static class Post {

        @Id
        @GeneratedValue
        private Long id;

        private String title;

        @OneToOne(mappedBy = "post", cascade = CascadeType.ALL, fetch = FetchType.EAGER, optional = false)
        private PostDetails details;

        public Post() {
        }

        public Post(String title) {
            this.title = title;
        }

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

        public PostDetails getDetails() {
            return details;
        }

        public void setDetails(PostDetails details) {
            if (details == null) {
                if (this.details != null) this.details.setPost(null);
            } else details.setPost(this);
            this.details = details;
        }
    }

    @Entity(name = "PostDetails")
    @Table(name = "post_details")
    @org.hibernate.annotations.Cache(usage = CacheConcurrencyStrategy.READ_WRITE)
    public static class PostDetails {

        @Id
        @GeneratedValue
        private Long id;

        @Column(name = "created_on")
        private Date createdOn;

        @Column(name = "created_by")
        private String createdBy;

        @OneToOne(fetch = FetchType.EAGER)
        @JoinColumn(name = "post_id")
        private Post post;

        public PostDetails() {
        }

        public PostDetails(String createdBy) {
            createdOn = new Date();
            this.createdBy = createdBy;
        }

        public Long getId() {
            return id;
        }

        public void setId(Long id) {
            this.id = id;
        }

        public Date getCreatedOn() {
            return createdOn;
        }

        public String getCreatedBy() {
            return createdBy;
        }

        public Post getPost() {
            return post;
        }

        public void setPost(Post post) {
            this.post = post;
        }
    }
}
