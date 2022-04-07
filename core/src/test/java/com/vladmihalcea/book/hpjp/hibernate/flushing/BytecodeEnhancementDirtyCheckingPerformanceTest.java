package com.vladmihalcea.book.hpjp.hibernate.flushing;

import com.codahale.metrics.MetricRegistry;
import com.codahale.metrics.Slf4jReporter;
import com.codahale.metrics.Timer;
import com.vladmihalcea.book.hpjp.hibernate.forum.Post;
import com.vladmihalcea.book.hpjp.hibernate.forum.PostComment;
import com.vladmihalcea.book.hpjp.hibernate.forum.PostDetails;
import com.vladmihalcea.book.hpjp.hibernate.forum.Tag;
import com.vladmihalcea.book.hpjp.util.AbstractTest;
import org.hibernate.EmptyInterceptor;
import org.hibernate.Interceptor;
import org.hibernate.type.Type;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import jakarta.persistence.EntityManager;
import java.io.Serializable;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
 * @author Vlad Mihalcea
 */
@RunWith(Parameterized.class)
public class BytecodeEnhancementDirtyCheckingPerformanceTest extends AbstractTest {

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

    public BytecodeEnhancementDirtyCheckingPerformanceTest(int entityCount) {
        this.entityCount = entityCount;
    }

    @Parameterized.Parameters
    public static Collection<Integer[]> rdbmsDataSourceProvider() {
        List<Integer[]> counts = new ArrayList<>();
        counts.add(new Integer[] {5});
        counts.add(new Integer[] {10});
        counts.add(new Integer[] {20});
        counts.add(new Integer[] {50});
        counts.add(new Integer[] {100});
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
                Post post = new Post()
                    .setId(i * 10L)
                    .setTitle("JPA with Hibernate")
                    .setDetails(
                        new PostDetails()
                            .setCreatedOn(new Date())
                            .setCreatedBy("Vlad MIhalcea")
                    )
                    .addComment(
                        new PostComment()
                            .setId(i * 10L)
                            .setReview("Good")
                    )
                    .addComment(
                        new PostComment()
                            .setId(i * 10L + 1)
                            .setReview("Excellent")
                    );

                Tag tag1 = new Tag()
                    .setId(i * 10L)
                    .setName("Java");

                Tag tag2 = new Tag()
                    .setId(i * 10L + 1)
                    .setName("Hibernate");

                entityManager.persist(post);

                entityManager.persist(tag1);
                entityManager.persist(tag2);

                post.getTags().add(tag1);
                post.getTags().add(tag2);

                entityManager.flush();
                postIds.add(post.getId());
            }
        });
    }

    @Test
    @Ignore
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
            enableMetrics = true;
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

}
