package com.vladmihalcea.hpjp.hibernate.fetching;

import com.codahale.metrics.MetricRegistry;
import com.codahale.metrics.Slf4jReporter;
import com.codahale.metrics.Timer;
import com.vladmihalcea.hpjp.util.AbstractMySQLIntegrationTest;
import com.vladmihalcea.hpjp.util.providers.DataSourceProvider;
import com.vladmihalcea.hpjp.util.providers.MySQLDataSourceProvider;
import jakarta.persistence.Entity;
import jakarta.persistence.EntityManager;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import org.hibernate.jpa.AvailableHints;
import org.hibernate.query.Query;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.util.Properties;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.LongStream;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * @author Vlad Mihalcea
 */
public class MySQLScrollableResultsStreamingCustomFetchSizeTest extends AbstractMySQLIntegrationTest {

    private MetricRegistry metricRegistry = new MetricRegistry();

    private Timer timer = metricRegistry.timer(getClass().getSimpleName());

    private Slf4jReporter logReporter = Slf4jReporter
            .forRegistry(metricRegistry)
            .outputTo(LOGGER)
            .build();

    @Override
    protected DataSourceProvider dataSourceProvider() {
        MySQLDataSourceProvider dataSourceProvider = new MySQLDataSourceProvider();
        dataSourceProvider.setUseCursorFetch(true);
        return dataSourceProvider;
    }

    @Override
    protected Class<?>[] entities() {
        return new Class[]{
            Post.class
        };
    }

    public void afterInit() {
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

    @Disabled
    @ParameterizedTest
    @ValueSource(ints = {1,2,5,10,25,50,75,100,250,500,750,1000,1500,2000,2500,5000})
    public void testStream(int resultSetSize) {
        //warming up
        LOGGER.info("Warming up");
        doInJPA(entityManager -> {
            for (int i = 0; i < 25_000; i++) {
                stream(entityManager, resultSetSize);
            }
        });
        int iterations = 10_000;
        doInJPA(entityManager -> {
            for (int i = 0; i < iterations; i++) {
                long startNanos = System.nanoTime();
                stream(entityManager, resultSetSize);
                timer.update(System.nanoTime() - startNanos, TimeUnit.NANOSECONDS);
            }
        });
        logReporter.report();
    }

    private void stream(EntityManager entityManager, int resultSetSize) {
        final AtomicLong sum = new AtomicLong();
        try(Stream<Post> postStream = entityManager
                .createQuery("select p from Post p", Post.class)
                .setMaxResults(resultSetSize)
                .setHint(AvailableHints.HINT_FETCH_SIZE, resultSetSize)
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
