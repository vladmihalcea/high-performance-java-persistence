package com.vladmihalcea.book.hpjp.hibernate.cache.query;

import com.vladmihalcea.book.hpjp.util.AbstractTest;
import org.hibernate.annotations.CacheConcurrencyStrategy;
import org.hibernate.jpa.QueryHints;
import org.hibernate.query.NativeQuery;
import org.junit.After;
import org.junit.Test;

import jakarta.persistence.*;
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
    protected void additionalProperties(Properties properties) {
        properties.put("hibernate.cache.use_second_level_cache", Boolean.TRUE.toString());
        properties.put("hibernate.cache.region.factory_class", "jcache");
        properties.put("hibernate.cache.use_query_cache", Boolean.TRUE.toString());
    }

    public void afterInit() {
        doInJPA(entityManager -> {
            entityManager.persist(
                new Post()
                    .setId(1L)
                    .setTitle("High-Performance Java Persistence")
                    .addComment(
                        new PostComment()
                            .setId(1L)
                            .setReview("JDBC part review")
                    )
            );
        });
    }

    @After
    public void destroy() {
        entityManagerFactory().getCache().evictAll();
        super.destroy();
    }

    public List<PostComment> getLatestPostComments(EntityManager entityManager) {
        return entityManager.createQuery("""
            select pc
            from PostComment pc
            order by pc.post.id desc
            """, PostComment.class)
        .setMaxResults(10)
        .setHint(QueryHints.HINT_CACHEABLE, true)
        .getResultList();
    }
    
    private List<PostComment> getLatestPostCommentsByPostId(EntityManager entityManager) {
        return entityManager.createQuery("""
            select pc
            from PostComment pc
            where pc.post.id = :postId
            """, PostComment.class)
        .setParameter("postId", 1L)
        .setMaxResults(10)
        .setHint(QueryHints.HINT_CACHEABLE, true)
        .getResultList();
    }
    
    private List<PostComment> getLatestPostCommentsByPost(EntityManager entityManager) {
        Post post = entityManager.find(Post.class, 1L);
        return entityManager.createQuery("""
            select pc
            from PostComment pc
            where pc.post = :post
            """, PostComment.class)
        .setParameter("post", post)
        .setMaxResults(10)
        .setHint(QueryHints.HINT_CACHEABLE, true)
        .getResultList();
    }

    private List<PostCommentSummary> getPostCommentSummaryByPost(EntityManager entityManager, Long postId) {
        return entityManager.createQuery("""
            select new
               com.vladmihalcea.book.hpjp.hibernate.cache.query.PostCommentSummary(
                   pc.id,
                   p.title,
                   pc.review
               )
            from PostComment pc
            left join pc.post p
            where p.id = :postId
            """, PostCommentSummary.class)
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
            Post post = entityManager.find(Post.class, 1L);
            post.addComment(
                new PostComment()
                    .setId(2L)
                    .setReview("JDBC part review")
            );
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

            entityManager.createNativeQuery("""
                UPDATE post 
                SET title = '\"'||title||'\"'
                """)
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
            entityManager.createNativeQuery("""
                UPDATE post 
                SET title = '\"'||title||'\"'
                """)
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

        @OneToMany(cascade = CascadeType.ALL, mappedBy = "post", orphanRemoval = true)
        @org.hibernate.annotations.Cache(usage = CacheConcurrencyStrategy.READ_WRITE)
        private List<PostComment> comments = new ArrayList<>();

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

        public List<PostComment> getComments() {
            return comments;
        }

        public Post addComment(PostComment comment) {
            comments.add(comment);
            comment.setPost(this);
            return this;
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
