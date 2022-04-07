package com.vladmihalcea.book.hpjp.hibernate.fetching;

import com.codahale.metrics.MetricRegistry;
import com.codahale.metrics.Slf4jReporter;
import com.codahale.metrics.Timer;
import com.codahale.metrics.UniformReservoir;
import com.vladmihalcea.book.hpjp.util.AbstractTest;
import org.hibernate.jpa.QueryHints;
import org.hibernate.query.*;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import jakarta.persistence.*;
import jakarta.persistence.Query;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;
import java.util.stream.LongStream;

/**
 * @author Vlad Mihalcea
 */
@RunWith(Parameterized.class)
public class PlanCacheSizePerformanceTest extends AbstractTest {

    private MetricRegistry metricRegistry = new MetricRegistry();

    private Timer timer = new Timer(new UniformReservoir(10000));

    private Slf4jReporter logReporter = Slf4jReporter
            .forRegistry(metricRegistry)
            .outputTo(LOGGER)
            .convertDurationsTo(TimeUnit.MICROSECONDS)
            .build();

    private final int planCacheMaxSize;

    public PlanCacheSizePerformanceTest(int planCacheMaxSize) {
        this.planCacheMaxSize = planCacheMaxSize;
    }

    @Parameterized.Parameters
    public static Collection<Integer[]> rdbmsDataSourceProvider() {
        List<Integer[]> planCacheMaxSizes = new ArrayList<>();
        planCacheMaxSizes.add(new Integer[] {1});
        planCacheMaxSizes.add(new Integer[] {100});
        return planCacheMaxSizes;
    }

    @Override
    protected void additionalProperties(Properties properties) {
        properties.put(
            "hibernate.query.plan_cache_max_size",
            planCacheMaxSize
        );

        properties.put(
            "hibernate.query.plan_parameter_metadata_max_size",
            planCacheMaxSize
        );
    }

    @Override
    protected Class<?>[] entities() {
        return new Class<?>[]{
            Post.class,
            PostComment.class,
        };
    }


    @Override
    public void init() {
        metricRegistry.register(getClass().getSimpleName(), timer);
        super.init();
        int commentsSize = 5;
        doInJPA(entityManager -> {
            LongStream.range(0, 50).forEach(i -> {
                Post post = new Post();
                post.setId(i);
                post.setTitle(String.format("Post nr. %d", i));

                LongStream.range(0, commentsSize).forEach(j -> {
                    PostComment comment = new PostComment();
                    comment.setId((i * commentsSize) + j);
                    comment.setReview(String.format("Good review nr. %d", comment.getId()));
                    post.addComment(comment);

                });
                entityManager.persist(post);
            });
        });
    }

    @Test
    @Ignore
    public void testEntityQueries() {
        compileQueries(this::getEntityQuery1, this::getEntityQuery2);
    }

    @Test
    @Ignore
    public void testNativeQueries() {
        compileQueries(this::getNativeQuery1, this::getNativeQuery2);
    }

    protected void compileQueries(
            Function<EntityManager, Query> query1,
            Function<EntityManager, Query> query2) {

        LOGGER.info("Warming up");

        doInJPA(entityManager -> {
            for (int i = 0; i < 10000; i++) {
                query1.apply(entityManager);
                query2.apply(entityManager);
            }
        });

        LOGGER.info("Compile queries for plan cache size {}", planCacheMaxSize);

        doInJPA(entityManager -> {
            for (int i = 0; i < 2500; i++) {
                long startNanos = System.nanoTime();
                query1.apply(entityManager);
                timer.update(System.nanoTime() - startNanos, TimeUnit.NANOSECONDS);

                startNanos = System.nanoTime();
                query2.apply(entityManager);
                timer.update(System.nanoTime() - startNanos, TimeUnit.NANOSECONDS);
            }
        });

        logReporter.report();
    }

    protected Query getEntityQuery1(EntityManager entityManager) {
        return entityManager.createQuery(
            "select new " +
            "   com.vladmihalcea.book.hpjp.hibernate.fetching.PostCommentSummary( " +
            "       p.id, p.title, c.review ) " +
            "from PostComment c " +
            "join c.post p")
        .setFirstResult(10)
        .setMaxResults(20)
        .setHint(QueryHints.HINT_FETCH_SIZE, 20);
    }

    protected Query getEntityQuery2(EntityManager entityManager) {
        return entityManager.createQuery(
            "select c " +
            "from PostComment c " +
            "join fetch c.post p " +
            "where p.title like :title"
        );
    }

    protected Query getNativeQuery1(EntityManager entityManager) {
        return entityManager.createNativeQuery(
            "select p.id, p.title, c.review * " +
            "from post_comment c " +
            "join post p on p.id = c.post_id ")
        .setFirstResult(10)
        .setMaxResults(20)
        .setHint(QueryHints.HINT_FETCH_SIZE, 20);
    }

    protected Query getNativeQuery2(EntityManager entityManager) {
        return entityManager.createNativeQuery(
            "select c.*, p.* " +
            "from post_comment c " +
            "join post p on p.id = c.post_id " +
            "where p.title like :title")
        .unwrap(NativeQuery.class)
        .addEntity(PostComment.class)
        .addEntity(Post.class);
    }

    @Entity(name = "Post")
    @Table(name = "post")

    public static class Post {

        @Id
        private Long id;

        private String title;

        @OneToMany(cascade = CascadeType.ALL, mappedBy = "post",
                orphanRemoval = true)
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
    @NamedNativeQuery(
            name = "findPostComments",
            query = "select c.review " +
                    "from post_comment c " +
                    "where c.id > :id "
    )
    @NamedNativeQuery(
            name = "findPostCommentSummary",
            query = "select c.review " +
                    "from post_comment c " +
                    "left join post p on p.id = c.post_id " +
                    "where p.title > :title "
    )
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
