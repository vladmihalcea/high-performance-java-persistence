package com.vladmihalcea.hpjp.hibernate.index.postgres;

import com.vladmihalcea.hpjp.util.AbstractTest;
import com.vladmihalcea.hpjp.util.providers.Database;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import org.junit.Test;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Properties;
import java.util.concurrent.ThreadLocalRandom;

/**
 * @author Vlad Mihalcea
 */
public class PostgreSQLBRINIndexTest extends AbstractTest {

    public static final int ROW_COUNT = 1_000;
    public static final int BATCH_SIZE = 500;

    @Override
    protected Class<?>[] entities() {
        return new Class<?>[] {
            Post.class
        };
    }

    @Override
    protected Database database() {
        return Database.POSTGRESQL;
    }

    @Override
    protected void additionalProperties(Properties properties) {
        properties.setProperty("hibernate.jdbc.batch_size", String.valueOf(BATCH_SIZE));
        properties.setProperty("hibernate.order_inserts", "true");
        properties.setProperty("hibernate.order_updates", "true");
    }

    @Override
    public void afterInit() {
        if(!ENABLE_LONG_RUNNING_TESTS) {
            return;
        }
        doInJPA(entityManager -> {
            ThreadLocalRandom random = ThreadLocalRandom.current();
            LocalDateTime timestamp = LocalDateTime.of(2024, 2, 29, 12, 0, 0);
            for (long i = 1; i <= ROW_COUNT; i++) {
                timestamp = timestamp.plusHours(6);
                entityManager.persist(
                    new Post()
                        .setId(i)
                        .setTitle(
                            String.format("Post nr. %d", i)
                        )
                        .setCreatedOn(timestamp)
                        .setCreatedBy(
                            random.nextInt(10) > 5 ? "Vlad Mihalcea" : "Alex Mihalcea"
                        )
                );

                if(i % BATCH_SIZE == 0) {
                    entityManager.flush();
                }
            }
        });

        executeStatement(
            "DROP INDEX IF EXISTS idx_post_created_on",
            """
            CREATE INDEX IF NOT EXISTS idx_post_created_on
            ON post USING BRIN(created_on)
            """,
            "ANALYZE VERBOSE"
        );
    }

    @Test
    public void testEquality() {
        List<String> executionPlanLines = doInJPA(entityManager -> {
            return entityManager.createNativeQuery("""
                EXPLAIN (ANALYZE, BUFFERS)
                SELECT p.title
                FROM post p
                WHERE p.created_on = :timestamp 
                """, String.class)
            .setParameter("timestamp", LocalDateTime.of(2024, 3, 4, 12, 0, 0))
            .getResultList();
        });

        LOGGER.info("Execution plan: \n{}", String.join("\n", executionPlanLines));
    }

    @Test
    public void testRangeScan() {
        List<String> executionPlanLines = doInJPA(entityManager -> {
            return entityManager.createNativeQuery("""
                EXPLAIN (ANALYZE, BUFFERS)
                SELECT p.title
                FROM post p
                WHERE 
                    p.created_on >= :startTimestamp AND
                    p.created_on < :endTimestamp
                """, String.class)
            .setParameter("startTimestamp", LocalDateTime.of(2024, 3, 4, 12, 0, 0))
            .setParameter("endTimestamp", LocalDateTime.of(2024, 3, 19, 0, 0, 0))
            .getResultList();
        });

        LOGGER.info("Execution plan: \n{}", String.join("\n", executionPlanLines));
    }

    @Test
    public void testOrderBy() {
        List<String> executionPlanLines = doInJPA(entityManager -> {
            return entityManager.createNativeQuery("""
                EXPLAIN (ANALYZE, BUFFERS)
                SELECT p.title
                FROM post p
                ORDER BY created_on DESC
                FETCH FIRST 10 ROWS ONLY
                """, String.class)
            .getResultList();
        });

        LOGGER.info("Execution plan: \n{}", String.join("\n", executionPlanLines));
    }

    @Test
    public void testTableScan() {
        List<String> executionPlanLines = doInJPA(entityManager -> {
            return entityManager.createNativeQuery("""
                EXPLAIN (ANALYZE, BUFFERS)
                SELECT p.id, p.title, p.created_by, p.created_on
                FROM post p
                WHERE 
                    p.created_by = :createdBy
                ORDER BY
                    p.created_on
                """, String.class)
            .setParameter("createdBy", "Vlad Mihalcea")
            .getResultList();
        });

        LOGGER.info("Execution plan: \n{}", String.join("\n", executionPlanLines));
    }

    @Entity(name = "Post")
    @Table(name = "post")
    public static class Post {

        @Id
        private Long id;

        private String title;

        @Column(name = "created_on")
        private LocalDateTime createdOn;

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

        public LocalDateTime getCreatedOn() {
            return createdOn;
        }

        public Post setCreatedOn(LocalDateTime createdOn) {
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
