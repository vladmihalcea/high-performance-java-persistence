package com.vladmihalcea.book.hpjp.hibernate.cache.readonly;

import com.vladmihalcea.book.hpjp.util.AbstractTest;
import org.hibernate.annotations.CacheConcurrencyStrategy;
import org.hibernate.annotations.Immutable;
import org.junit.Before;
import org.junit.Test;

import javax.persistence.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;


/**
 * @author Vlad Mihalcea
 */
public class ReadOnlyCacheConcurrencyStrategyImmutableTest extends AbstractTest {

    @Override
    protected Class<?>[] entities() {
        return new Class<?>[] {
            Post.class,
            PostComment.class
        };
    }

    @Override
    protected Properties properties() {
        Properties properties = super.properties();
        properties.put("hibernate.cache.use_second_level_cache", Boolean.TRUE.toString());
        properties.put("hibernate.cache.region.factory_class", "org.hibernate.cache.ehcache.EhCacheRegionFactory");
        return properties;
    }

    @Before
    public void init() {
        super.init();
        doInJPA(entityManager -> {
            Post post = new Post();
            post.setId(1L);
            post.setTitle("High-Performance Java Persistence");

            PostComment comment1 = new PostComment();
            comment1.setId(1L);
            comment1.setReview("JDBC part review");
            post.addComment(comment1);

            PostComment comment2 = new PostComment();
            comment2.setId(2L);
            comment2.setReview("Hibernate part review");
            post.addComment(comment2);

            entityManager.persist(post);
        });
        printCacheRegionStatistics(Post.class.getName());
        LOGGER.info("Post entity inserted");
    }

    @Test
    public void testReadOnlyEntityUpdate() {
        LOGGER.info("Read-only cache entries cannot be updated");
        doInJPA(entityManager -> {
            Post post = entityManager.find(Post.class, 1L);
            post.setTitle("High-Performance Hibernate");
        });
    }

    @Test
    public void testCollectionCacheUpdate() {

        LOGGER.info("Read-only collection cache entries cannot be updated");

        try {
            doInJPA(entityManager -> {
                Post post = entityManager.find(Post.class, 1L);
                PostComment comment = post.getComments().remove(0);
                comment.setPost(null);
            });
        } catch (Exception e) {
            LOGGER.error("Expected", e);
        }
    }

    @Entity(name = "Post")
    @org.hibernate.annotations.Cache(usage = CacheConcurrencyStrategy.READ_ONLY)
    @Immutable
    public static class Post {

        @Id
        private Long id;

        private String title;

        @Version
        private int version;

        @OneToMany(cascade = CascadeType.PERSIST, mappedBy = "post")
        @org.hibernate.annotations.Cache(usage = CacheConcurrencyStrategy.READ_ONLY)
        @Immutable
        private List<PostComment> comments = new ArrayList<>();

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

        public List<PostComment> getComments() {
            return comments;
        }

        public void addComment(PostComment comment) {
            comments.add(comment);
            comment.setPost(this);
        }
    }

    @Entity(name = "PostComment")
    @Table(name = "post_comment")
    @org.hibernate.annotations.Cache(usage = CacheConcurrencyStrategy.READ_ONLY)
    @Immutable
    public static class PostComment {

        @Id
        private Long id;

        @ManyToOne(fetch = FetchType.LAZY)
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
