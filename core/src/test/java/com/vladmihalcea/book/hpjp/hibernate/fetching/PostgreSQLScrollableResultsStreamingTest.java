package com.vladmihalcea.book.hpjp.hibernate.fetching;

import com.vladmihalcea.book.hpjp.util.AbstractPostgreSQLIntegrationTest;
import org.hibernate.Session;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.jpa.QueryHints;
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
                    statement.execute("CREATE INDEX idx_post_created_on ON post ( created_on DESC )");
                }
            });
            LongStream.range(0, 50 * 100).forEach(i -> {
                Post post = new Post(i);
                post.setTitle(String.format("Post nr. %d", i));
                entityManager.persist(post);
                if (i % 50 == 0 && i > 0) {
                    entityManager.flush();
                    entityManager.clear();
                }
            });
        });
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
            executeDML(entityManager, """
                SET session_preload_libraries = 'auto_explain';
                SET auto_explain.log_analyze TO ON;
                SET auto_explain.log_min_duration TO 1;
                """);

            List<Post> posts = entityManager.createQuery("""
                select p
                from Post p
                order by p.createdOn desc
                """, Post.class)
            .setHint(QueryHints.HINT_FETCH_SIZE, 50)
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
            List<String> executionPlanLines = entityManager.createNativeQuery("""
                EXPLAIN ANALYZE
                SELECT p
                FROM post p
                ORDER BY p.created_on DESC""")
            .setMaxResults(50)
            .getResultList();

            LOGGER.info("Execution plan: \n{}",
                String.join("\n", executionPlanLines)
            );
        });
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
