package com.vladmihalcea.book.hpjp.hibernate.cache;

import com.vladmihalcea.book.hpjp.util.AbstractTest;
import org.hibernate.SQLQuery;
import org.hibernate.annotations.CacheConcurrencyStrategy;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import javax.persistence.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

import static org.junit.Assert.assertEquals;

/**
 * QueryCacheTest - Test to check the 2nd level query cache
 *
 * @author Vlad Mihalcea
 */
public class QueryCacheTest extends AbstractTest {

    @Override
    protected Class<?>[] entities() {
        return new Class<?>[]{
            Post.class,
            PostComment.class,
        };
    }

    @Override
    protected Properties properties() {
        Properties properties = super.properties();
        properties.put("hibernate.cache.use_second_level_cache", Boolean.TRUE.toString());
        properties.put("hibernate.cache.region.factory_class", "org.hibernate.cache.ehcache.EhCacheRegionFactory");
        properties.put("hibernate.cache.use_query_cache", Boolean.TRUE.toString());
        return properties;
    }

    @Before
    public void init() {
        super.init();
        doInJPA(entityManager -> {
            Post post1 = new Post();
            post1.setId(1L);
            post1.setTitle("High-Performance Java Persistence");

            PostComment comment1 = new PostComment();
            comment1.setId(1L);
            comment1.setReview("JDBC part review");
            post1.addComment(comment1);

            entityManager.persist(post1);
        });
    }

    @After
    public void destroy() {
        entityManagerFactory().getCache().evictAll();
        super.destroy();
    }
    
    private List<PostComment> getLatestPostComments(EntityManager entityManager) {
        return entityManager.createQuery(
            "select pc " +
            "from PostComment pc " +
            "order by pc.post.id desc", PostComment.class)
        .setMaxResults(10)
        .setHint("org.hibernate.cacheable", true)
        .getResultList();
    }
    
    private List<PostComment> getLatestPostCommentsByPostId(EntityManager entityManager) {
        return entityManager.createQuery(
            "select pc " +
            "from PostComment pc " +
            "where pc.post.id = :postId", PostComment.class)
        .setParameter("postId", 1L)
        .setMaxResults(10)
        .setHint("org.hibernate.cacheable", true)
        .getResultList();
    }
    
    private List<PostComment> getLatestPostCommentsByPost(EntityManager entityManager) {
        Post post = entityManager.find(Post.class, 1L);
        return entityManager.createQuery(
            "select pc " +
            "from PostComment pc " +
            "where pc.post = :post ", PostComment.class)
            .setParameter("post", post)
        .setMaxResults(10)
        .setHint("org.hibernate.cacheable", true)
        .getResultList();
    }

    @Test
    public void testFindById() {
        doInJPA(entityManager -> {
            LOGGER.info("Evict regions and run query");
            entityManagerFactory().getCache().evictAll();
        });

        doInJPA(entityManager -> {
            List<Post> posts = entityManager
                .createQuery("select p from Post p where p.id = :id", Post.class)
                .setParameter("id", 1L)
                .setHint("org.hibernate.cacheable", true)
                .getResultList();
        });

        doInJPA(entityManager -> {
            List<Post> posts = entityManager
                .createQuery("select p from Post p where p.id = :id", Post.class)
                .setParameter("id", 1L)
                .setHint("org.hibernate.cacheable", true)
                .getResultList();
        });
    }

    @Test
    public void test2ndLevelCacheWithQuery() {
        doInJPA(entityManager -> {
            LOGGER.info("Evict regions and run query");
            entityManagerFactory().getCache().evictAll();
            assertEquals(1, getLatestPostComments(entityManager).size());
        });

        doInJPA(entityManager -> {
            LOGGER.info("Check get entity is cached");
            Post post = (Post) entityManager.find(Post.class, 1L);
        });

        doInJPA(entityManager -> {
            LOGGER.info("Check query is cached");
            assertEquals(1, getLatestPostComments(entityManager).size());
        });
    }

    @Test
    public void test2ndLevelCacheWithParameters() {
        doInJPA(entityManager -> {
            LOGGER.info("Query cache with basic type parameter");
            List<PostComment> comments = getLatestPostCommentsByPostId(entityManager);
            assertEquals(1, comments.size());
        });
        doInJPA(entityManager -> {
            LOGGER.info("Query cache with entity type parameter");
            List<PostComment> comments = getLatestPostCommentsByPost(entityManager);
            assertEquals(1, comments.size());
        });
    }

    @Test
    public void test2ndLevelCacheWithQueryInvalidation() {
        doInJPA(entityManager -> {
            Post post = entityManager.find(Post.class, 1L);
            assertEquals(1, getLatestPostComments(entityManager).size());

            LOGGER.info("Insert a new Post");
            PostComment newComment = new PostComment();
            newComment.setId(2L);
            newComment.setReview("JDBC part review");
            post.addComment(newComment);

            entityManager.persist(newComment);
            entityManager.flush();

            LOGGER.info("Query cache is invalidated");
            assertEquals(2, getLatestPostComments(entityManager).size());
        });

        doInJPA(entityManager -> {
            LOGGER.info("Check Query cache");
            assertEquals(2, getLatestPostComments(entityManager).size());
        });
    }

    @Test
    public void test2ndLevelCacheWithNativeQueryInvalidation() {
        doInJPA(entityManager -> {
            assertEquals(1, getLatestPostComments(entityManager).size());

            LOGGER.info("Execute native query");
            assertEquals(1, entityManager.createNativeQuery(
                "UPDATE post SET title = '\"'||title||'\"' "
            ).executeUpdate());

            LOGGER.info("Check query cache is invalidated");
            assertEquals(1, getLatestPostComments(entityManager).size());
        });
    }

    @Test
    public void test2ndLevelCacheWithNativeQuerySynchronization() {
        doInJPA(entityManager -> {
            assertEquals(1, getLatestPostComments(entityManager).size());

            LOGGER.info("Execute native query with synchronization");
            assertEquals(1, entityManager.createNativeQuery(
                    "UPDATE post SET title = '\"'||title||'\"' "
            )
            .unwrap(SQLQuery.class)
            .addSynchronizedEntityClass(Post.class)
            .executeUpdate());

            LOGGER.info("Check query cache is not invalidated");
            assertEquals(1, getLatestPostComments(entityManager).size());
        });
    }

    @Entity(name = "Post")
    @Table(name = "post")
    @org.hibernate.annotations.Cache(usage = CacheConcurrencyStrategy.READ_WRITE)
    public static class Post {

        @Id
        private Long id;

        private String title;

        @Version
        private int version;

        @OneToMany(cascade = CascadeType.ALL, mappedBy = "post", orphanRemoval = true)
        @org.hibernate.annotations.Cache(usage = CacheConcurrencyStrategy.READ_WRITE)
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
    @org.hibernate.annotations.Cache(usage = CacheConcurrencyStrategy.READ_WRITE)
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
