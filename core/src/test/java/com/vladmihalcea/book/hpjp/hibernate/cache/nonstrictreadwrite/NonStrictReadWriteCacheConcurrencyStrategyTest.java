package com.vladmihalcea.book.hpjp.hibernate.cache.nonstrictreadwrite;

import com.vladmihalcea.book.hpjp.util.AbstractTest;
import org.hibernate.annotations.CacheConcurrencyStrategy;
import org.junit.Before;
import org.junit.Test;

import javax.persistence.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;


/**
 * @author Vlad Mihalcea
 */
public class NonStrictReadWriteCacheConcurrencyStrategyTest extends AbstractTest {

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
        printCacheRegionStatistics(Post.class.getName() + ".comments");
        LOGGER.info("Post entity inserted");
    }

    @Test
    public void testPostEntityLoad() {

        LOGGER.info("Load Post entity and comments collection");

        doInJPA(entityManager -> {
            Post post = entityManager.find(Post.class, 1L);
            assertEquals(2, post.getComments().size());
            printCacheRegionStatistics(post.getClass().getName());
            printCacheRegionStatistics(Post.class.getName() + ".comments");
        });
    }

    @Test
    public void testPostEntityEvictModifyLoad() {

        LOGGER.info("Evict, modify, load");

        doInJPA(entityManager -> {
            Post post = entityManager.find(Post.class, 1L);
            entityManager.detach(post);

            post.setTitle("High-Performance Hibernate");
            entityManager.merge(post);
            entityManager.flush();

            entityManager.detach(post);
            post = entityManager.find(Post.class, 1L);
            printCacheRegionStatistics(post.getClass().getName());
        });
    }

    @Test
    public void testEntityUpdate() {
        doInJPA(entityManager -> {
            Post post = entityManager.find(Post.class, 1L);
            assertEquals(2, post.getComments().size());
        });

        doInJPA(entityManager -> {
            Post post = entityManager.find(Post.class, 1L);
            post.setTitle("High-Performance Hibernate");
            PostComment comment = post.getComments().remove(0);
            comment.setPost(null);
        });
        printCacheRegionStatistics(Post.class.getName());
        printCacheRegionStatistics(Post.class.getName() + ".comments");
        printCacheRegionStatistics(PostComment.class.getName());
    }

    @Test
    public void testNonVersionedEntityUpdate() {
        doInJPA(entityManager -> {
            PostComment comment = entityManager.find(PostComment.class, 1L);
        });
        printCacheRegionStatistics(PostComment.class.getName());
        doInJPA(entityManager -> {
            PostComment comment = entityManager.find(PostComment.class, 1L);
            comment.setReview("JDBC and Database part review");
        });
        printCacheRegionStatistics(PostComment.class.getName());
    }

    @Test
    public void testEntityDelete() {
        LOGGER.info("Cache entries can be deleted");

        doInJPA(entityManager -> {
            Post post = entityManager.find(Post.class, 1L);
            assertEquals(2, post.getComments().size());
        });

        printCacheRegionStatistics(Post.class.getName());
        printCacheRegionStatistics(Post.class.getName() + ".comments");
        printCacheRegionStatistics(PostComment.class.getName());

        doInJPA(entityManager -> {
            Post post = entityManager.find(Post.class, 1L);
            entityManager.remove(post);
        });
        
        printCacheRegionStatistics(Post.class.getName());
        printCacheRegionStatistics(Post.class.getName() + ".comments");
        printCacheRegionStatistics(PostComment.class.getName());
        
        doInJPA(entityManager -> {
            Post post = entityManager.find(Post.class, 1L);
            assertNull(post);
        });
    }

    @Entity(name = "Post")
    @Table(name = "post")
    @org.hibernate.annotations.Cache(usage = CacheConcurrencyStrategy.NONSTRICT_READ_WRITE)
    public static class Post {

        @Id
        private Long id;

        private String title;

        @Version
        private int version;

        @OneToMany(cascade = CascadeType.ALL, mappedBy = "post", orphanRemoval = true)
        @org.hibernate.annotations.Cache(usage = CacheConcurrencyStrategy.NONSTRICT_READ_WRITE)
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
    @org.hibernate.annotations.Cache(usage = CacheConcurrencyStrategy.NONSTRICT_READ_WRITE)
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
