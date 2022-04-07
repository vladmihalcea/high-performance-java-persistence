package com.vladmihalcea.book.hpjp.hibernate.cache.query;

import com.vladmihalcea.book.hpjp.util.AbstractTest;
import org.hibernate.annotations.CacheConcurrencyStrategy;
import org.hibernate.jpa.QueryHints;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import jakarta.persistence.*;
import java.util.List;
import java.util.Properties;

import static org.junit.Assert.assertEquals;

/**
 * @author Vlad Mihalcea
 */
public class QueryCacheNPlus1Test extends AbstractTest {

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
            Post post = new Post();
            post.setTitle("High-Performance Java Persistence");
            entityManager.persist(post);

            PostComment part1 = new PostComment();
            part1.setReview("Part one - JDBC");
            part1.setPost(post);
            entityManager.persist(part1);

            PostComment part2 = new PostComment();
            part2.setReview("Part two - Hibernate");
            part2.setPost(post);
            entityManager.persist(part2);

            PostComment part3 = new PostComment();
            part3.setReview("Part two - jOOQ");
            part3.setPost(post);
            entityManager.persist(part3);
        });
    }

    @After
    public void destroy() {
        entityManagerFactory().getCache().evictAll();
        super.destroy();
    }

    public List<PostComment> getLatestPostComments(
            EntityManager entityManager) {
        return entityManager.createQuery("""
            select pc
            from PostComment pc
            order by pc.post.id desc
            """, PostComment.class)
        .setMaxResults(10)
        .setHint(QueryHints.HINT_CACHEABLE, true)
        .getResultList();
    }

    @Test
    public void test2ndLevelCacheWithQuery() {
        doInJPA(entityManager -> {
            printQueryCacheRegionStatistics();
            assertEquals(3, getLatestPostComments(entityManager).size());

            printQueryCacheRegionStatistics();
            assertEquals(3, getLatestPostComments(entityManager).size());
        });
    }

    @Test
    public void test2ndLevelCacheWithQueryNPlus1() {
        doInJPA(entityManager -> {
            printQueryCacheRegionStatistics();
            assertEquals(3, getLatestPostComments(entityManager).size());
            printQueryCacheRegionStatistics();
        });

        doInJPA(entityManager -> {
            entityManager.getEntityManagerFactory().getCache().evict(PostComment.class);
        });

        doInJPA(entityManager -> {
            assertEquals(3, getLatestPostComments(entityManager).size());
        });
    }

    @Entity(name = "Post")
    @Table(name = "post")
    @org.hibernate.annotations.Cache(usage = CacheConcurrencyStrategy.READ_WRITE)
    public static class Post {

        @Id
        @GeneratedValue
        private Long id;

        private String title;

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

    @Entity(name = "PostComment")
    @Table(name = "post_comment")
    @org.hibernate.annotations.Cache(usage = CacheConcurrencyStrategy.READ_WRITE)
    public static class PostComment {

        @Id
        @GeneratedValue
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
