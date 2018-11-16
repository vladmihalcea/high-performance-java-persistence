package com.vladmihalcea.book.hpjp.hibernate.fetching;

import com.codahale.metrics.MetricRegistry;
import com.codahale.metrics.Slf4jReporter;
import com.vladmihalcea.book.hpjp.util.AbstractPostgreSQLIntegrationTest;
import org.hibernate.Session;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.jpa.QueryHints;
import org.hibernate.query.Query;
import org.junit.Test;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Table;
import java.sql.Statement;
import java.util.Date;
import java.util.List;
import java.util.Properties;
import java.util.stream.Collectors;
import java.util.stream.LongStream;
import java.util.stream.Stream;

import static org.junit.Assert.assertEquals;

/**
 * @author Vlad Mihalcea
 */
public class PostgreSQLScrollableResultsStreamingTest extends AbstractPostgreSQLIntegrationTest {

    private MetricRegistry metricRegistry = new MetricRegistry();

    private Slf4jReporter logReporter = Slf4jReporter
            .forRegistry(metricRegistry)
            .outputTo(LOGGER)
            .build();


    @Override
    protected Class<?>[] entities() {
        return new Class[]{
            Post.class
        };
    }

    @Override
    public void init() {
        super.init();
        doInJPA(entityManager -> {
            entityManager.unwrap( Session.class ).doWork( connection -> {
                try(Statement statement = connection.createStatement()) {
                    statement.execute( "CREATE INDEX idx_post_created_on ON post ( created_on DESC )" );
                }
            } );
            LongStream.range(0, 50 * 100).forEach(i -> {
                Post post = new Post(i);
                post.setTitle(String.format("Post nr. %d", i));
                entityManager.persist(post);
                if(i % 50 == 0 && i > 0) {
                    entityManager.flush();
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
    public void testStream() {
        List<Post> posts = doInJPA(entityManager -> {
            try(Stream<Post> postStream = entityManager
                .createQuery(
                    "select p " +
                    "from Post p " +
                    "order by p.createdOn desc", Post.class)
                .setHint(QueryHints.HINT_FETCH_SIZE, 50)
                .unwrap(Query.class)
                .stream()
            ) {
                return postStream
                    .limit(50)
                    .collect(Collectors.toList());
            }
        });

        assertEquals(50, posts.size());
    }

    @Test
    public void testStreamExecutionPlan() {
        List<String> executionPlanLines = doInJPA(entityManager -> {
            try(Stream<String> postStream = entityManager
                .createNativeQuery(
                    "EXPLAIN ANALYZE " +
                    "SELECT p " +
                    "FROM post p " +
                    "ORDER BY p.created_on DESC")
                .setHint(QueryHints.HINT_FETCH_SIZE, 50)
                .getResultStream()
            ) {
                return postStream.limit(50).collect(Collectors.toList());
            }
        });

        LOGGER.info( "Execution plan: {}",
                    executionPlanLines
                    .stream()
                    .collect(Collectors.joining( "\n" ))
        );
    }

    @Test
    public void testPaginationExecutionPlan() {
        List<String> executionPlanLines = doInJPA(entityManager -> {
            return entityManager
                .createNativeQuery(
                    "EXPLAIN ANALYZE " +
                    "SELECT p " +
                    "FROM post p " +
                    "ORDER BY p.created_on DESC")
                .setMaxResults( 50 )
                .getResultList();
        });

        LOGGER.info( "Execution plan: {}",
                     executionPlanLines
                     .stream()
                     .collect( Collectors.joining( "\n" ) )
        );
    }

    @Test
    public void testStreamWithoutMaxResult() {
        List<Post> posts = doInJPA(entityManager -> {
            try(Stream<Post> postStream = entityManager
                .createQuery(
                    "select p " +
                    "from Post p " +
                    "order by p.createdOn desc", Post.class)
                .unwrap(Query.class)
                .stream()
            ) {
                return postStream.limit( 50 ).collect( Collectors.toList() );
            }
        });

        assertEquals(50, posts.size());
    }

    @Entity(name = "Post")
    @Table(
        name = "post"
    )
    public static class Post {

        @Id
        private Long id;

        private String title;

        @Column(name = "created_on")
        @CreationTimestamp
        private Date createdOn;

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
