package com.vladmihalcea.book.hpjp.hibernate.cache.query;

import com.vladmihalcea.book.hpjp.hibernate.forum.dto.PostDTO;
import com.vladmihalcea.book.hpjp.hibernate.query.dto.projection.jpa.compact.JPADTOProjectionClassImportIntegratorPropertyClassTest;
import com.vladmihalcea.book.hpjp.util.AbstractTest;
import com.vladmihalcea.book.hpjp.util.providers.Database;
import com.vladmihalcea.hibernate.type.util.ClassImportIntegrator;
import org.hibernate.annotations.CacheConcurrencyStrategy;
import org.hibernate.integrator.spi.Integrator;
import org.hibernate.jpa.QueryHints;
import org.hibernate.jpa.boot.spi.IntegratorProvider;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import jakarta.persistence.*;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Properties;

import static org.junit.Assert.assertEquals;

/**
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
    protected Database database() {
        return Database.POSTGRESQL;
    }

    @Override
    protected void additionalProperties(Properties properties) {
        properties.put("hibernate.cache.use_second_level_cache", Boolean.TRUE.toString());
        properties.put("hibernate.cache.region.factory_class", "jcache");
        properties.put("hibernate.cache.use_query_cache", Boolean.TRUE.toString());
        properties.put(
            "hibernate.integrator_provider",
            ClassImportIntegratorIntegratorProvider.class
        );
    }

    public void afterInit() {
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
        List<PostSummary> latestPosts = entityManager.createQuery("""
            select new PostSummary(p.id, p.title, p.createdOn, count(pc.id))
            from PostComment pc
            left join pc.post p
            group by p.id, p.title
            order by p.createdOn desc
            """, PostSummary.class)
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

    public static class ClassImportIntegratorIntegratorProvider implements IntegratorProvider {

        @Override
        public List<Integrator> getIntegrators() {
            return List.of(
                new ClassImportIntegrator(
                    List.of(
                        PostSummary.class
                    )
                )
            );
        }
    }
}
