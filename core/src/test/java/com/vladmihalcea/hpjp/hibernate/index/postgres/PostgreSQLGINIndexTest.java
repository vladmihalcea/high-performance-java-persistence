package com.vladmihalcea.hpjp.hibernate.index.postgres;

import com.vladmihalcea.hpjp.util.AbstractTest;
import com.vladmihalcea.hpjp.util.RandomUtils;
import com.vladmihalcea.hpjp.util.providers.Database;
import org.hibernate.Session;
import org.junit.Test;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Properties;

/**
 * @author Vlad Mihalcea
 */
public class PostgreSQLGINIndexTest extends AbstractTest {

    public static final int ROW_COUNT = 5000;
    public static final int BATCH_SIZE = 500;

    @Override
    protected Class<?>[] entities() {
        return new Class<?>[]{
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
    protected void afterInit() {
        doInJPA(entityManager -> {
            entityManager.persist(
                new Book()
                    .setId(1L)
                    .setTitle("High-Performance Java Persistence")
                    .setAuthor("Vlad Mihalcea")
                    .setProperties("""
                        {
                           "publisher": "Amazon",
                           "price": 44.99,
                           "reviews": [
                               {
                                   "reviewer": "Cristiano",
                                   "review": "Excellent book to understand Java Persistence",
                                   "date": "2017-11-14",
                                   "rating": 5
                               },
                               {
                                   "reviewer": "T.W",
                                   "review": "The best JPA ORM book out there",
                                   "date": "2019-01-27",
                                   "rating": 5
                               },
                               {
                                   "reviewer": "Shaikh",
                                   "review": "The most informative book",
                                   "date": "2016-12-24",
                                   "rating": 4
                               }
                           ]
                        }
                        """)
            );

            for (long i = 2; i <= ROW_COUNT; i++) {
                LocalDateTime timestamp = LocalDateTime.of(2024, 2, 29, 12, 0, 0);
                entityManager.persist(
                    new Book()
                        .setId(i)
                        .setTitle(RandomUtils.randomTitle())
                        .setProperties(
                            String.format("""
                                {
                                   "reviews": [
                                     {
                                       "reviewer": "Reviewer id: %1$d",
                                       "review": "Review: %1$d",
                                       "date": "Date: %2$s",
                                       "rating": "Rating: %1$d"
                                     }
                                   ]
                                }
                                    """, i, timestamp.plusHours(i)
                            )
                        )
                );
            }
        });
    }
    
    @Test
    public void testGin() {
        executeStatement(
            "DROP INDEX IF EXISTS idx_book_properties_gin",
            """
            CREATE INDEX idx_book_properties_gin
            ON book USING GIN (properties)
            """,
            "ANALYZE VERBOSE"
        );

        List<String> executionPlanLines = doInJPA(entityManager -> {
            return entityManager.unwrap(Session.class)
                .doReturningWork(connection -> selectColumnList(
                    connection,
                    """
                    EXPLAIN (ANALYZE, BUFFERS)
                    SELECT title, author, published_on
                    FROM book
                    WHERE
                      properties ? 'publisher'
                    """, String.class
                )
            );
        });

        LOGGER.info("Execution plan: \n{}", String.join("\n", executionPlanLines));

        executeStatement(
            "DROP INDEX IF EXISTS idx_book_properties_gin"
        );
    }

    @Test
    public void testGinJsonbPathOps() {
        executeStatement(
            "DROP INDEX IF EXISTS idx_book_properties_gin",
            """
            CREATE INDEX idx_book_properties_gin
            ON book USING GIN (properties jsonb_path_ops)
            """,
            "ANALYZE VERBOSE"
        );

        List<String> executionPlanLines = doInJPA(entityManager -> {
            return entityManager.createNativeQuery("""
                EXPLAIN (ANALYZE, BUFFERS)
                SELECT
                  title, author, published_on
                FROM book
                WHERE
                  properties @> '{"title": "High-Performance Java Persistence"}'
                """, String.class)
            .getResultList();
        });

        LOGGER.info("Execution plan: \n{}", String.join("\n", executionPlanLines));

        executeStatement(
            "DROP INDEX IF EXISTS idx_book_properties_gin"
        );
    }

    @Test
    public void testGinPathExpression() {
        executeStatement(
            "DROP INDEX IF EXISTS idx_book_properties_gin",
            """
            CREATE INDEX idx_book_properties_gin
            ON book USING GIN ((properties -> 'reviews'))
            """,
            "ANALYZE VERBOSE"
        );

        List<String> executionPlanLines = doInJPA(entityManager -> {
            return entityManager
                .unwrap(Session.class)
                .doReturningWork(
                    connection -> selectColumnList(
                        connection,
                        """
                        EXPLAIN (ANALYZE, BUFFERS)
                        SELECT
                          title, author, published_on
                        FROM book
                        WHERE
                          properties -> 'reviews' @> '[{"rating":5}]'
                        """, String.class
                    )
                );
        });

        LOGGER.info("Execution plan: \n{}", String.join("\n", executionPlanLines));

        executeStatement(
            "DROP INDEX IF EXISTS idx_book_properties_gin"
        );
    }
}
