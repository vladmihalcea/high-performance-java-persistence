package com.vladmihalcea.book.hpjp.hibernate.flushing;

import com.codahale.metrics.MetricRegistry;
import com.codahale.metrics.Slf4jReporter;
import com.codahale.metrics.Timer;
import com.vladmihalcea.book.hpjp.util.AbstractTest;
import org.hibernate.EmptyInterceptor;
import org.hibernate.Interceptor;
import org.hibernate.type.Type;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import javax.persistence.*;
import java.io.Serializable;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
 * @author Vlad Mihalcea
 */
@RunWith(Parameterized.class)
public class DefaultDirtyCheckingPerformanceTest extends AbstractTest {

    private MetricRegistry metricRegistry = new MetricRegistry();

    private Timer timer = metricRegistry.timer(getClass().getSimpleName());

    private Slf4jReporter logReporter = Slf4jReporter
        .forRegistry(metricRegistry)
        .outputTo(LOGGER)
        .build();

    private int entityCount = 1;
    private int iterationCount = 1000;
    private List<Long> postIds = new ArrayList<>();
    private boolean enableMetrics = false;

    public DefaultDirtyCheckingPerformanceTest(int entityCount) {
        this.entityCount = entityCount;
    }

    @Parameterized.Parameters
    public static Collection<Integer[]> rdbmsDataSourceProvider() {
        List<Integer[]> counts = new ArrayList<>();
        counts.add(new Integer[] {5});
        counts.add(new Integer[] {10});
/*        counts.add(new Integer[] {20});
        counts.add(new Integer[] {50});
        counts.add(new Integer[] {100});*/
        return counts;
    }

    @Override
    protected Class<?>[] entities() {
        return new Class<?>[] {
            Post.class,
            PostDetails.class,
            PostComment.class,
            Tag.class
        };
    }

    @Override
    protected Interceptor interceptor() {
        return new EmptyInterceptor() {
            private Long startNanos;

            @Override
            public void preFlush(Iterator entities) {
                startNanos = System.nanoTime();
            }

            @Override
            public boolean onFlushDirty(Object entity, Serializable id, Object[] currentState, Object[] previousState, String[] propertyNames, Type[] types) {
                if (enableMetrics) {
                    timer.update(System.nanoTime() - startNanos, TimeUnit.NANOSECONDS);
                }
                return false;
            }
        };
    }

    @Override
    protected Properties properties() {
        Properties properties = super.properties();
        properties.put("hibernate.order_inserts", "true");
        properties.put("hibernate.order_updates", "true");
        properties.put("hibernate.jdbc.batch_size", String.valueOf(50));
        return properties;
    }

    @Override
    public void init() {
        super.init();
        doInJPA(entityManager -> {
            for (int i = 0; i < entityCount; i++) {
                Post post = new Post("JPA with Hibernate");
                post.setId(i * 10L);

                PostDetails details = new PostDetails();
                details.setCreatedOn(new Date());
                details.setCreatedBy("Vlad");
                post.addDetails(details);

                Tag tag1 = new Tag();
                tag1.setId(i * 10L);
                tag1.setName("Java");
                Tag tag2 = new Tag();
                tag2.setId(i * 10L + 1);
                tag2.setName("Hibernate");

                entityManager.persist(post);

                entityManager.persist(tag1);
                entityManager.persist(tag2);

                post.getTags().add(tag1);
                post.getTags().add(tag2);

                PostComment comment1 = new PostComment();
                comment1.setId(i * 10L);
                comment1.setReview("Good");

                PostComment comment2 = new PostComment();
                comment2.setId(i * 10L + 1);
                comment2.setReview("Excellent");

                post.addComment(comment1);
                post.addComment(comment2);

                entityManager.flush();
                postIds.add(post.getId());
            }
        });
    }

    @Test
    public void testDirtyChecking() {
        doInJPA(entityManager -> {
            List<Post> posts = posts(entityManager);
            for (int i = 0; i < 100_000; i++) {
                for (Post post : posts) {
                    modifyEntities(post, i);
                }
                entityManager.flush();
            }
        });

        doInJPA(entityManager -> {
            enableMetrics= true;
            List<Post> posts = posts(entityManager);
            for (int i = 0; i < iterationCount; i++) {
                for (Post post : posts) {
                    modifyEntities(post, i);
                }
                entityManager.flush();
            }
            LOGGER.info("Flushed {} entities", entityCount);
            logReporter.report();
        });
    }

    private List<Post> posts(EntityManager entityManager) {
        return entityManager.createQuery(
            "select distinct pc " +
            "from PostComment pc " +
            "join fetch pc.post p " +
            "join fetch p.tags " +
            "join fetch p.details " +
            "where p.id in :ids", PostComment.class)
        .setParameter("ids", postIds)
        .getResultList()
        .stream()
        .map(PostComment::getPost)
        .distinct()
        .collect(Collectors.toList());
    }

    private void modifyEntities(Post post, int i) {
        String value = String.valueOf(i);
        post.setTitle(value);
        post.getTags().get(0).setName(value);
        post.getTags().get(1).setName(value);
        post.getDetails().setCreatedBy(value);
        post.getDetails().setCreatedOn(new Date(i));
        post.getComments().get(0).setReview(value);
    }

    @Entity(name = "Post")
    @Table(name = "post")
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

    @Entity(name = "Tag")
    @Table(name = "tag")
    public static class Tag {

        @Id
        private Long id;

        private String name;

        public Long getId() {
            return id;
        }

        public void setId(Long id) {
            this.id = id;
        }

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }
    }


}
