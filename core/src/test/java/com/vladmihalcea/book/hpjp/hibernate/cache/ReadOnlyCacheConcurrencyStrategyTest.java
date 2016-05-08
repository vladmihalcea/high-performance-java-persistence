package com.vladmihalcea.book.hpjp.hibernate.cache;

import com.codahale.metrics.MetricRegistry;
import com.codahale.metrics.Slf4jReporter;
import com.codahale.metrics.Timer;
import com.vladmihalcea.book.hpjp.util.AbstractTest;
import org.hibernate.annotations.CacheConcurrencyStrategy;
import org.junit.Before;
import org.junit.Test;

import javax.persistence.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

import static org.junit.Assert.*;


/**
 * ReadOnlyCacheConcurrencyStrategyTest - Test to check CacheConcurrencyStrategy.READ_ONLY
 *
 * @author Vlad Mihalcea
 */
public class ReadOnlyCacheConcurrencyStrategyTest extends AbstractTest {

    private MetricRegistry metricRegistry = new MetricRegistry();

    private Timer timer = metricRegistry.timer(getClass().getSimpleName());

    private Slf4jReporter logReporter = Slf4jReporter
            .forRegistry(metricRegistry)
            .outputTo(LOGGER)
            .build();

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
        properties.put("hibernate.cache.use_structured_entries", Boolean.TRUE.toString());
        
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
        });
    }

    @Test
    public void testPostEntityLoad() {

        LOGGER.info("Read-only entities are read-through");

        doInJPA(entityManager -> {
            Post post = entityManager.find(Post.class, 1L);
            assertNotNull(post);
        });

        printEntityCacheStats(Post.class.getName(), true);

        doInJPA(entityManager -> {
            LOGGER.info("Load Post from cache");
            entityManager.find(Post.class, 1L);
        });
        
        printEntityCacheStats(Post.class.getName(), true);

    }

    @Test
    public void testCollectionCache() {
        LOGGER.info("Collections require separate caching");
        doInJPA(entityManager -> {

            Post post = entityManager.find(Post.class, 1L);
            
            PostComment comment1 = new PostComment();
            comment1.setId(1L);
            comment1.setReview("JDBC part review");
            post.addComment(comment1);

            PostComment comment2 = new PostComment();
            comment2.setId(2L);
            comment2.setReview("Hibernate part review");
            post.addComment(comment2);
        });
        
        printEntityCacheStats(Post.class.getName());

        doInJPA(entityManager -> {
            LOGGER.info("Load PostComment from database ");
            Post post = entityManager.find(Post.class, 1L);
            assertEquals(2, post.getComments().size());
        });

        printEntityCacheStats(Post.class.getName());
        printEntityCacheStats(PostComment.class.getName());

        doInJPA(entityManager -> {
            LOGGER.info("Load PostComment from cache");
            Post post = entityManager.find(Post.class, 1L);
            assertEquals(2, post.getComments().size());
        });

        try {
            doInJPA(entityManager -> {
                LOGGER.info("Read-only collection cache entries cannot be updated");
                Post post = entityManager.find(Post.class, 1L);
                PostComment comment = post.getComments().remove(0);
                comment.setPost(null);
            });
        } catch (Exception e) {
            LOGGER.error("Expected", e);
        }
    }

    @Test
    public void testReadOnlyEntityUpdate() {
        try {
            LOGGER.info("Read-only cache entries cannot be updated");
            doInJPA(entityManager -> {
                Post post = entityManager.find(Post.class, 1L);
                printEntityCacheStats(Post.class.getName());
                post.setTitle("High-Performance Hibernate");
            });
        } catch (Exception e) {
            LOGGER.error("Expected", e);
        }
    }

    @Test
    public void testReadOnlyEntityDelete() {
        LOGGER.info("Read-only cache entries can be deleted");
        doInJPA(entityManager -> {
            Post post = entityManager.find(Post.class, 1L);
            assertNotNull(post);
            entityManager.remove(post);
        });
        
        printEntityCacheStats(Post.class.getName());
        
        doInJPA(entityManager -> {
            Post post = entityManager.find(Post.class, 1L);
            printEntityCacheStats(Post.class.getName());
            assertNull(post);
        });
    }

    @Entity(name = "Post")
    @org.hibernate.annotations.Cache(usage = CacheConcurrencyStrategy.READ_ONLY)
    public static class Post {

        @Id
        private Long id;

        private String title;

        @Version
        private int version;

        @OneToMany(cascade = CascadeType.ALL, mappedBy = "post", orphanRemoval = true)
        @org.hibernate.annotations.Cache(usage = CacheConcurrencyStrategy.READ_ONLY)
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
    @org.hibernate.annotations.Cache(usage = CacheConcurrencyStrategy.READ_ONLY)
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
