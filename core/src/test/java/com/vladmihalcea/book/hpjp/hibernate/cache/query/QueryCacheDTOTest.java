package com.vladmihalcea.book.hpjp.hibernate.cache.query;

import com.vladmihalcea.book.hpjp.util.AbstractTest;
import org.hibernate.annotations.CacheConcurrencyStrategy;
import org.hibernate.jpa.QueryHints;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import javax.persistence.*;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Properties;

import static org.junit.Assert.assertEquals;

/**
 * QueryCacheTest - Test to check the 2nd level query cache
 *
 * @author Vlad Mihalcea
 */
public class QueryCacheDTOTest extends AbstractTest {

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
            for (int i = 0; i < 10; i++) {
                Post post = new Post();
                post.setTitle(
                    String.format("High-Performance Java Persistence, Chapter %d", i + 1)
                );

                int commentCount = (int) (Math.random() * 10);

                for (int j = 0; j < commentCount; j++) {
                    PostComment comment = new PostComment();
                    comment.setReview(
                        String.format("Comment %d", j + 1)
                    );

                    post.addComment(comment);
                }
                entityManager.persist(post);
            }
        });
    }

    @After
    public void destroy() {
        entityManagerFactory().getCache().evictAll();
        super.destroy();
    }

    @Test
    public void test2ndLevelDtoProjection() {
        doInJPA(entityManager -> {
            List<PostSummary> latestPosts = getLatestPostSummaries(
                entityManager,
                5,
                false
            );

            assertEquals(5, latestPosts.size());
        });

        doInJPA(entityManager -> {
            List<PostSummary> latestPosts = getLatestPostSummaries(
                entityManager,
                5,
                false
            );

            assertEquals(5, latestPosts.size());
        });

        doInJPA(entityManager -> {
            List<PostSummary> latestPosts = getLatestPostSummaries(
                entityManager,
                5,
                true
            );

            printQueryCacheRegionStatistics();
            assertEquals(5, latestPosts.size());
        });

        doInJPA(entityManager -> {
            List<PostSummary> latestPosts = getLatestPostSummaries(
                entityManager,
                5,
                true
            );

            printQueryCacheRegionStatistics();
            assertEquals(5, latestPosts.size());
        });
    }

    List<PostSummary> getLatestPostSummaries(
                EntityManager entityManager,
                int maxResults,
                boolean cacheable) {
        List<PostSummary> latestPosts = entityManager.createQuery(
            "select new " +
            "   com.vladmihalcea.book.hpjp.hibernate.cache.query.PostSummary(" +
            "       p.id, " +
            "       p.title, " +
            "       p.createdOn, " +
            "       count(pc.id) " +
            "   ) " +
            "from PostComment pc " +
            "left join pc.post p " +
            "group by p.id, p.title " +
            "order by p.createdOn desc ", PostSummary.class)
        .setMaxResults(maxResults)
        .setHint(QueryHints.HINT_CACHEABLE, cacheable)
        .getResultList();

        LOGGER.debug("Latest posts: {}", latestPosts);

        return latestPosts;
    }

    @Entity(name = "Post")
    @Table(name = "post")
    public static class Post {

        @Id
        @GeneratedValue
        private Long id;

        private String title;

        @Temporal(TemporalType.TIMESTAMP)
        @Column(name = "created_on")
        private Date createdOn = new Date();

        @OneToMany(cascade = CascadeType.ALL, mappedBy = "post", orphanRemoval = true)
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

        public Date getCreatedOn() {
            return createdOn;
        }

        public void setCreatedOn(Date createdOn) {
            this.createdOn = createdOn;
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
