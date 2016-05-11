package com.vladmihalcea.book.hpjp.hibernate.cache;

import com.codahale.metrics.MetricRegistry;
import com.codahale.metrics.Slf4jReporter;
import com.codahale.metrics.Timer;
import com.vladmihalcea.book.hpjp.util.AbstractTest;
import org.hibernate.annotations.CacheConcurrencyStrategy;
import org.hibernate.annotations.Immutable;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import javax.persistence.*;
import java.io.Serializable;
import java.util.*;
import java.util.concurrent.TimeUnit;

import static org.junit.Assert.assertNotNull;


/**
 * @author Vlad Mihalcea
 */
@RunWith(Parameterized.class)
public class HydratedStateBenchmarkTest extends AbstractTest {

    private MetricRegistry metricRegistry = new MetricRegistry();

    private Timer timer = metricRegistry.timer(getClass().getSimpleName());

    private Slf4jReporter logReporter = Slf4jReporter
            .forRegistry(metricRegistry)
            .outputTo(LOGGER)
            .build();

    private int insertCount;

    public HydratedStateBenchmarkTest(int insertCount) {
        this.insertCount = insertCount;
    }

    @Parameterized.Parameters
    public static Collection<Object[]> dataProvider() {
        List<Object[]> providers = new ArrayList<>();
        providers.add(new Object[]{100});
        providers.add(new Object[]{500});
        providers.add(new Object[]{1000});
        providers.add(new Object[]{5000});
        providers.add(new Object[]{10000});
        return providers;
    }

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
        properties.put("hibernate.cache.region.factory_class", "org.hibernate.cache.ehcache.EhCacheRegionFactory");

        properties.put("hibernate.jdbc.batch_size", "100");
        properties.put("hibernate.order_inserts", "true");
        return properties;
    }

    @Before
    public void init() {
        super.init();
        doInJPA(entityManager -> {
            for (long i = 0; i < insertCount; i++) {
                Post post = new Post();
                post.setId(i);
                post.setTitle("High-Performance Java Persistence");
                entityManager.persist(post);
/*
                PostDetails details = new PostDetails();
                details.setCreatedBy("Vlad Mihalcea");
                details.setCreatedOn(new Date());
                details.setPost(post);
                entityManager.persist(details);*/
            }
        });
    }

    @Test
    public void testReadOnlyFetchPerformance() {
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
        private int version;

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
}
