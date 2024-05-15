package com.vladmihalcea.hpjp.hibernate.index.postgres;

import com.vladmihalcea.hpjp.util.AbstractTest;
import com.vladmihalcea.hpjp.util.RandomUtils;
import com.vladmihalcea.hpjp.util.exception.ExceptionUtil;
import com.vladmihalcea.hpjp.util.providers.Database;
import org.hibernate.cfg.AvailableSettings;
import org.hibernate.exception.ConstraintViolationException;
import org.junit.Ignore;
import org.junit.Test;

import java.util.List;
import java.util.Properties;

import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

/**
 * @author Vlad Mihalcea
 */
public class PostgreSQLHashIndexTest extends AbstractTest {

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
        properties.setProperty(AvailableSettings.HBM2DDL_AUTO, "none");
    }

    @Override
    protected void beforeInit() {
        executeStatement(
            "DROP TABLE IF EXISTS book CASCADE",
            """
            CREATE TABLE book (
                id bigint not null,
                title varchar(100),
                author varchar(50),
                published_on timestamp(6),
                properties jsonb,
                PRIMARY KEY (id)
            )
            """
        );
    }

    @Override
    public void afterInit() {
        doInJPA(entityManager -> {
            for (long i = 1; i <= ROW_COUNT; i++) {
                entityManager.persist(
                    new Book()
                        .setId(i)
                        .setTitle(RandomUtils.randomTitle())
                        .setAuthor("Vlad Mihalcea")
                );

                if(i % BATCH_SIZE == 0) {
                    entityManager.flush();
                }
            }
        });

        executeStatement(
            "DROP INDEX IF EXISTS idx_book_title_hash",
            "DROP INDEX IF EXISTS idx_book_title_btree",
            """
            CREATE INDEX IF NOT EXISTS idx_book_title_hash
            ON book USING HASH (title)
            """,
            """
            CREATE INDEX IF NOT EXISTS idx_book_title_btree
            ON book (title)
            """,
            "ANALYZE VERBOSE"
        );
    }

    @Test
    public void testEquality() {
        List<String> executionPlanLines = doInJPA(entityManager -> {
            Book book = entityManager.find(Book.class, RandomUtils.GENERATOR.nextLong(ROW_COUNT));
            String title = book.getTitle();

            return entityManager.createNativeQuery("""
                EXPLAIN (ANALYZE, BUFFERS)
                SELECT title, author, published_on
                FROM book
                WHERE title = :title 
                """, String.class)
            .setParameter("title", title)
            .getResultList();
        });

        LOGGER.info("Execution plan: \n{}", String.join("\n", executionPlanLines));
    }

    @Test
    @Ignore
    public void testDuplicate() {
        try {
            doInJPA(entityManager -> {
                Book book = entityManager.find(Book.class, RandomUtils.GENERATOR.nextLong(ROW_COUNT));
                String title = book.getTitle();

                entityManager.persist(
                    new Book()
                        .setId(1L + ROW_COUNT)
                        .setTitle(title)
                );
            });
            fail("Should thrown ConstraintViolationException!");
        } catch (Exception expected) {
            assertTrue(ExceptionUtil.isCausedBy(expected, ConstraintViolationException.class));
        }
    }
}
