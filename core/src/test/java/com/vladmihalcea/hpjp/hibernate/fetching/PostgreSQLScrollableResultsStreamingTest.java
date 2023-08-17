package com.vladmihalcea.hpjp.hibernate.fetching;

import com.vladmihalcea.hpjp.util.AbstractPostgreSQLIntegrationTest;
import org.hibernate.Session;
import org.hibernate.jpa.AvailableHints;
import org.junit.Test;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Tuple;
import java.sql.Statement;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.Date;
import java.util.List;
import java.util.Properties;
import java.util.stream.Collectors;
import java.util.stream.LongStream;

import static org.junit.Assert.assertEquals;

/**
 * @author Vlad Mihalcea
 */
public class PostgreSQLScrollableResultsStreamingTest extends AbstractPostgreSQLIntegrationTest {

    @Override
    protected Class<?>[] entities() {
        return new Class[]{
            Post.class
        };
    }

    @Override
    public void afterInit() {
        doInJPA(entityManager -> {
            entityManager.unwrap(Session.class).doWork(connection -> {
                try (Statement statement = connection.createStatement()) {
                    statement.execute("CREATE INDEX idx_post_created_on ON post (created_on DESC)");
                }
            });

            LocalDateTime startTimestamp = LocalDateTime.now();
            LongStream.rangeClosed(1, 50 * 100).forEach(i -> {
                entityManager.persist(
                    new Post()
                        .setId(i)
                        .setTitle(String.format("Post nr. %d", i))
                        .setCreatedOn(Timestamp.valueOf(startTimestamp.plusHours(i)))
                );
                if (i % 50 == 0 && i > 0) {
                    entityManager.flush();
                    entityManager.clear();
                }
            });
        });

        executeStatement("VACUUM FULL ANALYZE");
    }

    @Override
    protected void additionalProperties(Properties properties) {
        properties.put("hibernate.jdbc.batch_size", "50");
        properties.put("hibernate.order_inserts", "true");
        properties.put("hibernate.order_updates", "true");
    }

    @Test
    public void testStreamExecutionPlan() {
        doInJPA(entityManager -> {
            executeStatement(entityManager, """
                SET auto_explain.log_min_duration TO 0;
                """);

            List<Tuple> posts = (List<Tuple>) entityManager.createNativeQuery("""
                SELECT id, title, created_on
                FROM post
                ORDER BY created_on DESC
                """, Tuple.class)
                .setHint(AvailableHints.HINT_FETCH_SIZE, 50)
                .getResultStream()
                .limit(50)
                .collect(Collectors.toList());

            assertEquals(50, posts.size());

            //Read the execution plan from $PG_DATA/log/postgresql-yyyy-mm-dd_HHmmss.log
        });
    }

    @Test
    public void testPaginationExecutionPlan() {
        doInJPA(entityManager -> {
            executeStatement(entityManager, """
                SET auto_explain.log_min_duration TO 0;
                """);

            List<Tuple> posts = (List<Tuple>) entityManager.createNativeQuery("""
                SELECT id, title, created_on
                FROM post
                ORDER BY created_on DESC
                """)
                .setMaxResults(50)
                .getResultList();

            assertEquals(50, posts.size());
        });

        //Read the execution plan from $PG_DATA/log/postgresql-yyyy-mm-dd_HHmmss.log
    }

    @Test
    public void testStreamWithoutMaxResult() {
        doInJPA(entityManager -> {
            List<Post> posts = entityManager.createQuery("""
                select p
                from Post p
                order by p.createdOn desc
                """, Post.class)
                .getResultStream()
                .limit(50)
                .collect(Collectors.toList());

            assertEquals(50, posts.size());
        });
    }

    @Entity(name = "Post")
    @Table(name = "post")
    public static class Post {

        @Id
        private Long id;

        private String title;

        @Column(name = "created_on")
        private Date createdOn;

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
    }
}
