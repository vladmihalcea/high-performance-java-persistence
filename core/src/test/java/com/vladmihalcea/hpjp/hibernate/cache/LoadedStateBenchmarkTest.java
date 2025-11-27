package com.vladmihalcea.hpjp.hibernate.cache;

import com.codahale.metrics.MetricRegistry;
import com.codahale.metrics.Slf4jReporter;
import com.codahale.metrics.Timer;
import com.vladmihalcea.hpjp.util.AbstractTest;
import jakarta.persistence.*;
import org.hibernate.annotations.CacheConcurrencyStrategy;
import org.hibernate.annotations.Immutable;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.io.Serializable;
import java.util.Date;
import java.util.Properties;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertNotNull;


/**
 * @author Vlad Mihalcea
 */
public class LoadedStateBenchmarkTest extends AbstractTest {

    private MetricRegistry metricRegistry = new MetricRegistry();

    private Timer timer = metricRegistry.timer(getClass().getSimpleName());

    private Slf4jReporter logReporter = Slf4jReporter
            .forRegistry(metricRegistry)
            .outputTo(LOGGER)
            .build();

    @Override
    protected Class<?>[] entities() {
        return new Class<?>[] {
            Post.class,
            PostDetails.class
        };
    }

    @Override
    protected Properties properties() {
        Properties properties = super.properties();
        properties.put("hibernate.cache.use_second_level_cache", Boolean.TRUE.toString());
        properties.put("hibernate.cache.region.factory_class", "jcache");

        properties.put("hibernate.jdbc.batch_size", "100");
        properties.put("hibernate.order_inserts", "true");
        return properties;
    }

    public void addData(int insertCount) {
        doInJPA(entityManager -> {
            for (long i = 0; i < insertCount; i++) {
                Post post = new Post();
                post.setId(i);
                post.setTitle("High-Performance Java Persistence");
                entityManager.persist(post);
            }
        });
    }

    @ParameterizedTest
    @ValueSource(ints = {100, 500, 1_000, 5_000, 10_000})
    @Disabled
    public void testReadOnlyFetchPerformance(int insertCount) {
        addData(insertCount);

        //warming-up
        doInJPA(entityManager -> {
            for (long i = 0; i < 10000; i++) {
                Post post = entityManager.find(Post.class, i % insertCount);
                //PostDetails details = entityManager.find(PostDetails.class, i);
                assertNotNull(post);
            }
        });
        doInJPA(entityManager -> {
            long startNanos = System.nanoTime();
            for (long i = 0; i < insertCount; i++) {
                Post post = entityManager.find(Post.class, i);
                //PostDetails details = entityManager.find(PostDetails.class, i);
            }
            timer.update(System.nanoTime() - startNanos, TimeUnit.NANOSECONDS);
        });
        logReporter.report();
    }


    @Entity(name = "Post")
    @org.hibernate.annotations.Cache(usage = CacheConcurrencyStrategy.READ_WRITE)
    @org.hibernate.annotations.Immutable
    public static class Post implements Serializable {

        @Id
        private Long id;

        private String title;

        @Version
        private short version;

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

    @Entity(name = "PostDetails")
    @org.hibernate.annotations.Cache(usage = CacheConcurrencyStrategy.READ_WRITE)
    @Immutable
    //This does not work since it features an association type
    public static class PostDetails implements Serializable {

        @Id
        private Long id;

        @Column(name = "created_on")
        private Date createdOn;

        @Column(name = "created_by")
        private String createdBy;

        @OneToOne(fetch = FetchType.LAZY)
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
}
