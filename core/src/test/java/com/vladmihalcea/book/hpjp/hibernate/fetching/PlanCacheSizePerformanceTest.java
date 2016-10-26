package com.vladmihalcea.book.hpjp.hibernate.fetching;

import com.codahale.metrics.MetricRegistry;
import com.codahale.metrics.Slf4jReporter;
import com.codahale.metrics.Timer;
import com.codahale.metrics.UniformReservoir;
import com.vladmihalcea.book.hpjp.util.AbstractTest;
import org.hibernate.SQLQuery;
import org.hibernate.jpa.QueryHints;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import javax.persistence.*;
import java.util.*;
import java.util.concurrent.TimeUnit;
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
    protected Class<?>[] entities() {
        return new Class<?>[]{
            Post.class,
            PostDetails.class,
            PostComment.class,
            Tag.class
        };
    }


    @Override
    public void init() {
        metricRegistry.register(getClass().getSimpleName(), timer);
        super.init();
        int commentsSize = 5;
        doInJPA(entityManager -> {
            LongStream.range(0, 50).forEach(i -> {
                Post post = new Post(i);
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

    @Override
    protected Properties properties() {
        Properties properties = super.properties();
        properties.put("hibernate.query.plan_cache_max_size", planCacheMaxSize);
        properties.put("hibernate.query.plan_parameter_metadata_max_size", planCacheMaxSize);
        return properties;
    }

    @Test
    public void testEntityQueries() {
        //warming up
        LOGGER.info("Warming up");
        doInJPA(entityManager -> {
            for (int i = 0; i < 10000; i++) {
                getEntityQuery1(entityManager);
                getEntityQuery2(entityManager);
            }
        });
        LOGGER.info("Create entity queries for plan cache size {}", planCacheMaxSize);
        int iterations = 2500;
        doInJPA(entityManager -> {
            for (int i = 0; i < iterations; i++) {
                long startNanos = System.nanoTime();
                getEntityQuery1(entityManager);
                timer.update(System.nanoTime() - startNanos, TimeUnit.NANOSECONDS);
                startNanos = System.nanoTime();
                getEntityQuery2(entityManager);
                timer.update(System.nanoTime() - startNanos, TimeUnit.NANOSECONDS);
            }
        });
        logReporter.report();
    }

    @Test
    public void testNativeQueries() {
        //warming up
        LOGGER.info("Warming up");
        doInJPA(entityManager -> {
            for (int i = 0; i < 10000; i++) {
                getNativeQuery1(entityManager);
                getNativeQuery2(entityManager);
            }
        });
        LOGGER.info("Create native queries for plan cache size {}", planCacheMaxSize);
        int iterations = 2500;
        doInJPA(entityManager -> {
            for (int i = 0; i < iterations; i++) {
                long startNanos = System.nanoTime();
                getNativeQuery1(entityManager);
                timer.update(System.nanoTime() - startNanos, TimeUnit.NANOSECONDS);
                startNanos = System.nanoTime();
                getNativeQuery2(entityManager);
                timer.update(System.nanoTime() - startNanos, TimeUnit.NANOSECONDS);
            }
        });
        logReporter.report();
    }

    protected Object getEntityQuery1(EntityManager entityManager) {
        return createEntityQuery1(entityManager);
    }

    protected Object getEntityQuery2(EntityManager entityManager) {
        return createEntityQuery2(entityManager);
    }

    protected Object getNativeQuery1(EntityManager entityManager) {
        return createNativeQuery1(entityManager);
    }

    protected Object getNativeQuery2(EntityManager entityManager) {
        return createNativeQuery2(entityManager);
    }

    protected Query createEntityQuery1(EntityManager entityManager) {
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

    protected Query createEntityQuery2(EntityManager entityManager) {
        return entityManager.createQuery(
            "select c " +
            "from PostComment c " +
            "join fetch c.post p " +
            "where p.title like :title");
    }

    protected Query createNativeQuery1(EntityManager entityManager) {
        return entityManager.createNativeQuery(
            "select p.id, p.title, c.review * " +
            "from post_comment c " +
            "join post p on p.id = c.post_id ")
        .setFirstResult(10)
        .setMaxResults(20)
        .setHint(QueryHints.HINT_FETCH_SIZE, 20);
    }

    protected org.hibernate.Query createNativeQuery2(EntityManager entityManager) {
        return entityManager.createNativeQuery(
            "select c.*, p.* " +
            "from post_comment c " +
            "join post p on p.id = c.post_id " +
            "where p.title like :title")
        .unwrap(SQLQuery.class)
        .addEntity(PostComment.class)
        .addEntity(Post.class);
    }

    @Entity(name = "Post")
    @Table(name = "post")
    @NamedNativeQuery(
        name = "findPostCommentsByPostTitle",
        query = "select c.review " +
                "from post_comment c " +
                "where c.id > :id "
    )
    public static class Post {

        @Id
        private Long id;

        private String title;

        public Post() {
        }

        public Post(Long id) {
            this.id = id;
        }

        public Post(String title) {
            this.title = title;
        }

        @OneToMany(cascade = CascadeType.ALL, mappedBy = "post",
                orphanRemoval = true)
        private List<PostComment> comments = new ArrayList<>();

        @OneToOne(cascade = CascadeType.ALL, mappedBy = "post",
                orphanRemoval = true, fetch = FetchType.LAZY)
        private PostDetails details;

        @ManyToMany
        @JoinTable(name = "post_tag",
                joinColumns = @JoinColumn(name = "post_id"),
                inverseJoinColumns = @JoinColumn(name = "tag_id")
        )
        private List<Tag> tags = new ArrayList<>();

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

        public PostDetails getDetails() {
            return details;
        }

        public List<Tag> getTags() {
            return tags;
        }

        public void addComment(PostComment comment) {
            comments.add(comment);
            comment.setPost(this);
        }

        public void addDetails(PostDetails details) {
            this.details = details;
            details.setPost(this);
        }

        public void removeDetails() {
            this.details.setPost(null);
            this.details = null;
        }
    }

    @Entity(name = "PostDetails")
    @Table(name = "post_details")
    public static class PostDetails {

        @Id
        private Long id;

        @Column(name = "created_on")
        private Date createdOn;

        @Column(name = "created_by")
        private String createdBy;

        public PostDetails() {
            createdOn = new Date();
        }

        @OneToOne(fetch = FetchType.LAZY)
        @JoinColumn(name = "id")
        @MapsId
        private Post post;

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

        public Date getCreatedOn() {
            return createdOn;
        }

        public void setCreatedOn(Date createdOn) {
            this.createdOn = createdOn;
        }

        public String getCreatedBy() {
            return createdBy;
        }

        public void setCreatedBy(String createdBy) {
            this.createdBy = createdBy;
        }
    }

    @Entity(name = "PostComment")
    @Table(name = "post_comment")
    public static class PostComment {

        @Id
        private Long id;

        @ManyToOne
        private Post post;

        private String review;

        public PostComment() {
        }

        public PostComment(String review) {
            this.review = review;
        }

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

    @Entity(name = "Tag")
    @Table(name = "tag")
    public static class Tag {

        @Id
        private Long id;

        private String name;

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }
    }
}
