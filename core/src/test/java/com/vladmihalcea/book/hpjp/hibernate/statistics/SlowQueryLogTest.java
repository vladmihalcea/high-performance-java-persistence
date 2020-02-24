package com.vladmihalcea.book.hpjp.hibernate.statistics;

import com.vladmihalcea.book.hpjp.hibernate.fetching.CriteriaAPIEntityTypeJoinedTest;
import com.vladmihalcea.book.hpjp.hibernate.fetching.PostgreSQLScrollableResultsStreamingTest;
import com.vladmihalcea.book.hpjp.util.AbstractTest;
import com.vladmihalcea.book.hpjp.util.providers.entity.BlogEntityProvider;
import org.hibernate.Session;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.cfg.AvailableSettings;
import org.hibernate.engine.jdbc.connections.internal.DatasourceConnectionProviderImpl;
import org.hibernate.jpa.QueryHints;
import org.hibernate.resource.jdbc.spi.PhysicalConnectionHandlingMode;
import org.hibernate.stat.Statistics;
import org.hibernate.stat.internal.StatisticsInitiator;
import org.junit.Test;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Table;
import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Root;
import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.Date;
import java.util.List;
import java.util.Properties;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Collectors;
import java.util.stream.LongStream;
import java.util.stream.Stream;

import static com.vladmihalcea.book.hpjp.util.providers.entity.BlogEntityProvider.Post;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 * @author Vlad Mihalcea
 */
public class SlowQueryLogTest extends AbstractTest {

    @Override
    protected Class<?>[] entities() {
        return new Class[]{
            Post.class
        };
    }

    @Override
    public void afterInit() {
        doInJPA(entityManager -> {
            LongStream.rangeClosed(1, 50 * 100).forEach(i -> {
                entityManager.persist(
                    new Post()
                        .setId(i)
                        .setTitle(
                            String.format(
                                "High-Performance Java Persistence - Page %d review",
                                i
                            )
                        )
                        .setCreatedBy("Vlad Mihalcea")
                );
                if(i % 50 == 0 && i > 0) {
                    entityManager.flush();
                }
            });
        });
    }

    @Override
    protected void additionalProperties(Properties properties) {
        properties.put("hibernate.jdbc.batch_size", "50");
        properties.put("hibernate.order_inserts", "true");
        properties.put("hibernate.order_updates", "true");

        properties.put("hibernate.session.events.log.LOG_QUERIES_SLOWER_THAN_MS", "5");
    }

    @Test
    public void test() {
        LOGGER.info("Check slow JPQL query");
        
        {
            List<Post> posts = doInJPA(entityManager -> {
            try(Stream<Post> postStream = entityManager
                .createQuery(
                    "select p " +
                    "from Post p " +
                    "order by p.createdOn desc", Post.class)
                .setHint(QueryHints.HINT_FETCH_SIZE, 50)
                .getResultStream()
            ) {
                return postStream
                    .skip(4950)
                    .limit(50)
                    .collect(Collectors.toList());
            }
        });

            assertEquals(50, posts.size());
        }

        LOGGER.info("Check slow Criteria API query");


        {
            List<Post> posts = doInJPA(entityManager -> {
                CriteriaBuilder builder = entityManager.getCriteriaBuilder();

                CriteriaQuery<Post> postQuery = builder.createQuery(Post.class);
                Root<Post> post = postQuery.from(Post.class);

                postQuery
                    .orderBy(
                        builder.desc(post.get("createdOn"))
                    );
                
                try(Stream<Post> postStream = entityManager
                    .createQuery(postQuery)
                    .setHint(QueryHints.HINT_FETCH_SIZE, 50)
                    .getResultStream()
                ) {
                    return postStream
                        .skip(4950)
                        .limit(50)
                        .collect(Collectors.toList());
                }
            });

            assertEquals(50, posts.size());    
        }

        LOGGER.info("Check slow native SQL query");


        {
            List<Post> posts = doInJPA(entityManager -> {
                try(Stream<Post> postStream = entityManager
                    .createNativeQuery(
                        "SELECT p.* " +
                        "FROM post p " +
                        "ORDER BY p.created_on DESC", Post.class)
                    .setHint(QueryHints.HINT_FETCH_SIZE, 50)
                    .getResultStream()
                ) {
                    return postStream
                        .skip(4950)
                        .limit(50)
                        .collect(Collectors.toList());
                }
            });

            assertEquals(50, posts.size());

            assertEquals(50, posts.size());
        }
    }

    @Entity(name = "Post")
    @Table(name = "post")
    public static class Post {

        @Id
        private Long id;

        private String title;

        @Column(name = "created_on")
        @CreationTimestamp
        private Date createdOn;

        @Column(name = "created_by")
        private String createdBy;

        public Long getId() {
            return id;
        }

        public Post setId(Long id) {
            this.id = id;
            return this;
        }

        public String getTitle() {
            return title;
        }

        public Post setTitle(String title) {
            this.title = title;
            return this;
        }

        public Date getCreatedOn() {
            return createdOn;
        }

        public Post setCreatedOn(Date createdOn) {
            this.createdOn = createdOn;
            return this;
        }

        public String getCreatedBy() {
            return createdBy;
        }

        public Post setCreatedBy(String createdBy) {
            this.createdBy = createdBy;
            return this;
        }
    }
}
