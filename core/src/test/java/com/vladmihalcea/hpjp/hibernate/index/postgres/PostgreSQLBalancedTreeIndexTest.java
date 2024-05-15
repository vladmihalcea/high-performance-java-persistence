package com.vladmihalcea.hpjp.hibernate.index.postgres;

import com.vladmihalcea.hpjp.util.AbstractTest;
import com.vladmihalcea.hpjp.util.RandomUtils;
import com.vladmihalcea.hpjp.util.providers.Database;
import org.junit.Test;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Properties;

/**
 * @author Vlad Mihalcea
 */
public class PostgreSQLBalancedTreeIndexTest extends AbstractTest {

    public static final int ROW_COUNT = 5000;
    public static final int BATCH_SIZE = 500;

    @Override
    protected Class<?>[] entities() {
        return new Class<?>[] {
            Book.class
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
        doInJPA(entityManager -> {
            LocalDateTime timestamp = LocalDateTime.of(2024, 2, 29, 12, 0, 0);
            for (long i = 1; i <= ROW_COUNT; i++) {
                timestamp = timestamp.plusHours(6);
                entityManager.persist(
                    new Book()
                        .setId(i)
                        .setTitle(RandomUtils.randomTitle())
                        .setPublishedOn(timestamp)
                        .setAuthor(RandomUtils.GENERATOR.nextInt(10) > 5 ? "Vlad Mihalcea" : "Alex Mihalcea")
                );

                if(i % BATCH_SIZE == 0) {
                    entityManager.flush();
                }
            }
        });

        executeStatement(
            "DROP INDEX IF EXISTS idx_book_published_on",
            """
            CREATE INDEX IF NOT EXISTS idx_book_published_on
            ON book (published_on)
            INCLUDE (title)
            """,
            "ANALYZE VERBOSE"
        );
    }

    @Test
    public void testEquality() {
        List<String> executionPlanLines = doInJPA(entityManager -> {
            return entityManager.createNativeQuery("""
                EXPLAIN (ANALYZE, BUFFERS)
                SELECT title
                FROM book
                WHERE published_on = :timestamp 
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
                SELECT title
                FROM book
                WHERE 
                    published_on >= :startTimestamp AND
                    published_on < :endTimestamp
                """, String.class)
            .setParameter("startTimestamp", LocalDateTime.of(2024, 3, 1, 12, 0, 0))
            .setParameter("endTimestamp", LocalDateTime.of(2024, 3, 29, 18, 0, 0))
            .getResultList();
        });

        LOGGER.info("Execution plan: \n{}", String.join("\n", executionPlanLines));
    }

    @Test
    public void testOrderBy() {
        List<String> executionPlanLines = doInJPA(entityManager -> {
            return entityManager.createNativeQuery("""
                EXPLAIN (ANALYZE, BUFFERS)
                SELECT title
                FROM book
                ORDER BY published_on DESC
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
                SELECT id, title, author, published_on
                FROM book
                WHERE 
                    author = :author
                ORDER BY
                    published_on
                """, String.class)
            .setParameter("author", "Vlad Mihalcea")
            .getResultList();
        });

        LOGGER.info("Execution plan: \n{}", String.join("\n", executionPlanLines));
    }
}
