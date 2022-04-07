package com.vladmihalcea.book.hpjp.hibernate.fetching;

import com.codahale.metrics.MetricRegistry;
import com.codahale.metrics.Slf4jReporter;
import com.codahale.metrics.Timer;
import com.vladmihalcea.book.hpjp.util.AbstractMySQLIntegrationTest;
import org.hibernate.query.Query;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import jakarta.persistence.Entity;
import jakarta.persistence.EntityManager;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Properties;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.LongStream;
import java.util.stream.Stream;

import static org.junit.Assert.assertEquals;

/**
 * @author Vlad Mihalcea
 */
@RunWith(Parameterized.class)
public class MySQLScrollableResultsNoStreamingTest extends AbstractMySQLIntegrationTest {

    private MetricRegistry metricRegistry = new MetricRegistry();

    private Timer timer = metricRegistry.timer(getClass().getSimpleName());

    private Slf4jReporter logReporter = Slf4jReporter
            .forRegistry(metricRegistry)
            .outputTo(LOGGER)
            .build();

    private final int resultSetSize;

    public MySQLScrollableResultsNoStreamingTest(Integer resultSetSize) {
        this.resultSetSize = resultSetSize;
    }

    @Override
    protected Class<?>[] entities() {
        return new Class[]{
            Post.class
        };
    }

    @Parameterized.Parameters
    public static Collection<Integer[]> parameters() {
        List<Integer[]> providers = new ArrayList<>();
        providers.add(new Integer[]{1});
        providers.add(new Integer[]{2});
        providers.add(new Integer[]{5});
        providers.add(new Integer[]{10});
        providers.add(new Integer[]{25});
        providers.add(new Integer[]{50});
        providers.add(new Integer[]{75});
        providers.add(new Integer[]{100});
        providers.add(new Integer[]{250});
        providers.add(new Integer[]{500});
        providers.add(new Integer[]{750});
        providers.add(new Integer[]{1000});
        providers.add(new Integer[]{1500});
        providers.add(new Integer[]{2000});
        providers.add(new Integer[]{2500});
        providers.add(new Integer[]{5000});
        return providers;
    }

    @Override
    public void init() {
        super.init();
        doInJPA(entityManager -> {
            LongStream.range(0, 5000).forEach(i -> {
                Post post = new Post(i);
                post.setTitle(String.format("Post nr. %d", i));
                entityManager.persist(post);
                if(i % 50 == 0 && i > 0) {
                    entityManager.flush();
                    entityManager.clear();
                }
            });
        });
    }

    @Override
    protected Properties properties() {
        Properties properties = super.properties();
        properties.put("hibernate.jdbc.batch_size", "50");
        properties.put("hibernate.order_inserts", "true");
        properties.put("hibernate.order_updates", "true");
        return properties;
    }

    @Test
    @Ignore
    public void testStream() {
        //warming up
        LOGGER.info("Warming up");
        doInJPA(entityManager -> {
            for (int i = 0; i < 25_000; i++) {
                stream(entityManager);
            }
        });
        int iterations = 10_000;
        doInJPA(entityManager -> {
            for (int i = 0; i < iterations; i++) {
                long startNanos = System.nanoTime();
                stream(entityManager);
                timer.update(System.nanoTime() - startNanos, TimeUnit.NANOSECONDS);
            }
        });
        logReporter.report();
    }

    private void stream(EntityManager entityManager) {
        final AtomicLong sum = new AtomicLong();
        try(Stream<Post> postStream = entityManager
            .createQuery("select p from Post p", Post.class)
            .setMaxResults(resultSetSize)
            .unwrap(Query.class)
            .stream()) {
            postStream.forEach(post -> sum.incrementAndGet());
        }
        assertEquals(resultSetSize, sum.get());
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
}
