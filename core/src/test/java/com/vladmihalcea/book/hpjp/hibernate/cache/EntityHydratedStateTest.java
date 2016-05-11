package com.vladmihalcea.book.hpjp.hibernate.cache;

import com.vladmihalcea.book.hpjp.util.AbstractTest;
import org.hibernate.annotations.Cache;
import org.hibernate.annotations.CacheConcurrencyStrategy;
import org.junit.Before;
import org.junit.Test;

import javax.persistence.*;
import java.util.Date;
import java.util.Properties;

import static org.junit.Assert.assertNotNull;

/**
 * @author Vlad Mihalcea
 */
public class EntityHydratedStateTest extends AbstractTest {

    @Override
    protected Class<?>[] entities() {
        return new Class<?>[] {
            Post.class,
            PostDetails.class,
            PostComment.class
        };
    }

    @Override
    protected Properties properties() {
        Properties properties = super.properties();
        properties.put("hibernate.cache.region.factory_class", "org.hibernate.cache.ehcache.EhCacheRegionFactory");
        properties.put("hibernate.generate_statistics", Boolean.TRUE.toString());
        return properties;
    }

    @Before
    public void init() {
        super.init();
        doInJPA(entityManager -> {
            Post post = new Post();
            post.setId(1L);
            post.setTitle("High-Performance Java Persistence");
            entityManager.persist(post);

            PostDetails details = new PostDetails();
            details.setCreatedBy("Vlad Mihalcea");
            details.setCreatedOn(new Date());
            details.setPost(post);
            entityManager.persist(details);

            PostComment comment1 = new PostComment();
            comment1.setId(1L);
            comment1.setReview("JDBC part review");
            comment1.setPost(post);
            entityManager.persist(comment1);

            PostComment comment2 = new PostComment();
            comment2.setId(2L);
            comment2.setReview("Hibernate part review");
            comment2.setPost(post);
            entityManager.persist(comment2);
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
    @Cache(usage = CacheConcurrencyStrategy.READ_WRITE)
    public static class Post {

        @Id
        private Long id;

        private String title;

        @Version
        private int version;

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
        @JoinColumn(name = "id")
        @MapsId
        private Post post;

        public Long getId() {
            return id;
        }

        public void setId(Long id) {
            this.id = id;
        }

        public Post getPost() {
            return post;
        }

        public void setPost(Post post) {
            this.post = post;
        }

        public Date getCreatedOn() {
            return createdOn;
        }

        public void setCreatedOn(Date createdOn) {
            this.createdOn = createdOn;
        }

        public String getCreatedBy() {
            return createdBy;
        }

        public void setCreatedBy(String createdBy) {
            this.createdBy = createdBy;
        }
    }

    @Entity(name = "PostComment")
    @Cache(usage = CacheConcurrencyStrategy.READ_WRITE)
    public static class PostComment {

        @Id
        private Long id;

        @ManyToOne
        private Post post;

        private String review;

        public Long getId() {
            return id;
        }

        public void setId(Long id) {
            this.id = id;
        }

        public Post getPost() {
            return post;
        }

        public void setPost(Post post) {
            this.post = post;
        }

        public String getReview() {
            return review;
        }

        public void setReview(String review) {
            this.review = review;
        }
    }
}
