package com.vladmihalcea.book.hpjp.hibernate.cache.query;

import com.vladmihalcea.book.hpjp.util.AbstractTest;
import org.hibernate.annotations.CacheConcurrencyStrategy;
import org.hibernate.jpa.QueryHints;
import org.hibernate.query.NativeQuery;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import javax.persistence.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
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
        properties.put("hibernate.cache.region.factory_class", "ehcache");
        properties.put("hibernate.cache.use_query_cache", Boolean.TRUE.toString());
        return properties;
    }

    @Before
    public void init() {
        super.init();
        doInJPA(entityManager -> {
            Post post = new Post();
            post.setId(1L);
            post.setTitle("High-Performance Java Persistence");

            PostComment comment = new PostComment();
            comment.setId(1L);
            comment.setReview("JDBC part review");
            post.addComment(comment);

            entityManager.persist(post);
        });
    }

    @After
    public void destroy() {
        entityManagerFactory().getCache().evictAll();
        super.destroy();
    }

    public List<PostComment> getLatestPostComments(EntityManager entityManager) {
        return entityManager.createQuery(
            "select pc " +
            "from PostComment pc " +
            "order by pc.post.id desc", PostComment.class)
        .setMaxResults(10)
        .setHint(QueryHints.HINT_CACHEABLE, true)
        .getResultList();
    }
    
    private List<PostComment> getLatestPostCommentsByPostId(EntityManager entityManager) {
        return entityManager.createQuery(
            "select pc " +
            "from PostComment pc " +
            "where pc.post.id = :postId", PostComment.class)
        .setParameter("postId", 1L)
        .setMaxResults(10)
        .setHint(QueryHints.HINT_CACHEABLE, true)
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
        .setHint(QueryHints.HINT_CACHEABLE, true)
        .getResultList();
    }

    private List<PostCommentSummary> getPostCommentSummaryByPost(EntityManager entityManager, Long postId) {
        return entityManager.createQuery(
            "select new " +
            "   com.vladmihalcea.book.hpjp.hibernate.cache.query.PostCommentSummary(" +
            "       pc.id, " +
            "       p.title, " +
            "       pc.review" +
            "   ) " +
            "from PostComment pc " +
            "left join pc.post p " +
            "where p.id = :postId ", PostCommentSummary.class)
            .setParameter("postId", postId)
        .setMaxResults(10)
        .setHint(QueryHints.HINT_CACHEABLE, true)
        .getResultList();
    }

    @Test
    public void test2ndLevelCacheWithoutResults() {
        doInJPA(entityManager -> {
            entityManager.createQuery("delete from PostComment").executeUpdate();
        });
        doInJPA(entityManager -> {
            LOGGER.info("Query cache with basic type parameter");
            List<PostComment> comments = getLatestPostCommentsByPostId(entityManager);
            assertTrue(comments.isEmpty());
        });
        doInJPA(entityManager -> {
            LOGGER.info("Query cache with entity type parameter");
            List<PostComment> comments = getLatestPostCommentsByPostId(entityManager);
            assertTrue(comments.isEmpty());
        });
    }

    @Test
    public void test2ndLevelCacheWithQuery() {
        doInJPA(entityManager -> {
            printQueryCacheRegionStatistics();
            assertEquals(1, getLatestPostComments(entityManager).size());
            printQueryCacheRegionStatistics();
            assertEquals(1, getLatestPostComments(entityManager).size());
        });
    }

    @Test
    public void test2ndLevelCacheWithQueryEntityLoad() {
        doInJPA(entityManager -> {
            printCacheRegionStatistics(PostComment.class.getName());
            printQueryCacheRegionStatistics();

            assertEquals(1, getLatestPostComments(entityManager).size());

            printCacheRegionStatistics(PostComment.class.getName());
            printQueryCacheRegionStatistics();

            executeSync(() -> {
                doInJPA(_entityManager -> {
                    List<PostComment> _comments = getLatestPostComments(_entityManager);
                    assertEquals(1, _comments.size());

                    _comments.get(0).setReview("Revision 2");
                });
            });

            printCacheRegionStatistics(PostComment.class.getName());
            printQueryCacheRegionStatistics();
            List<PostComment> comments = getLatestPostComments(entityManager);
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
    public void test2ndLevelCacheWithProjection() {
        Long postId = 1L;

        doInJPA(entityManager -> {
            LOGGER.info("Query cache with projection");
            List<PostCommentSummary> comments = getPostCommentSummaryByPost(entityManager, postId);
            printQueryCacheRegionStatistics();
            assertEquals(1, comments.size());
        });
        doInJPA(entityManager -> {
            LOGGER.info("Query cache with projection");
            List<PostCommentSummary> comments = getPostCommentSummaryByPost(entityManager, postId);
            assertEquals(1, comments.size());
            printQueryCacheRegionStatistics();
        });
    }

    @Test
    public void test2ndLevelCacheWithQueryInvalidation() {
        doInJPA(entityManager -> {

            assertEquals(1, getLatestPostComments(entityManager).size());
            printQueryCacheRegionStatistics();

            LOGGER.info("Insert a new PostComment");
            PostComment newComment = new PostComment();
            newComment.setId(2L);
            newComment.setReview("JDBC part review");
            Post post = entityManager.find(Post.class, 1L);
            post.addComment(newComment);
            entityManager.flush();

            assertEquals(2, getLatestPostComments(entityManager).size());
            printQueryCacheRegionStatistics();
        });

        LOGGER.info("After transaction commit");
        printQueryCacheRegionStatistics();

        doInJPA(entityManager -> {
            LOGGER.info("Check query cache");
            assertEquals(2, getLatestPostComments(entityManager).size());
        });
        printQueryCacheRegionStatistics();
    }

    @Test
    public void test2ndLevelCacheWithNativeQueryInvalidation() {
        doInJPA(entityManager -> {
            assertEquals(1, getLatestPostComments(entityManager).size());
            printQueryCacheRegionStatistics();

            int postCount = ((Number) entityManager.createNativeQuery(
                "SELECT count(*) FROM post")
                .getSingleResult()).intValue();

            assertEquals(postCount, getLatestPostComments(entityManager).size());
            printQueryCacheRegionStatistics();
        });
    }

    @Test
    public void test2ndLevelCacheWithNativeUpdateStatementInvalidation() {
        doInJPA(entityManager -> {
            assertEquals(1, getLatestPostComments(entityManager).size());
            printQueryCacheRegionStatistics();

            entityManager.createNativeQuery(
                "UPDATE post SET title = '\"'||title||'\"' ")
            .executeUpdate();

            assertEquals(1, getLatestPostComments(entityManager).size());
            printQueryCacheRegionStatistics();
        });
    }

    @Test
    public void test2ndLevelCacheWithNativeUpdateStatementSynchronization() {
        doInJPA(entityManager -> {
            assertEquals(1, getLatestPostComments(entityManager).size());
            printQueryCacheRegionStatistics();

            LOGGER.info("Execute native query with synchronization");
            entityManager.createNativeQuery(
                "UPDATE post SET title = '\"'||title||'\"' ")
            .unwrap(NativeQuery.class)
            .addSynchronizedEntityClass(Post.class)
            .executeUpdate();

            assertEquals(1, getLatestPostComments(entityManager).size());
            printQueryCacheRegionStatistics();
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
