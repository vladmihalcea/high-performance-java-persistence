package com.vladmihalcea.hpjp.jdbc.caching;

import com.vladmihalcea.hpjp.util.AbstractTest;
import com.vladmihalcea.hpjp.util.providers.Database;
import com.vladmihalcea.hpjp.util.providers.queries.PostgreSQLQueries;
import jakarta.persistence.*;
import org.hibernate.annotations.JdbcType;
import org.hibernate.dialect.PostgreSQLEnumJdbcType;
import org.junit.Test;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.Assert.fail;

/**
 * @author Vlad Mihalcea
 */
public class PostgreSQLPlanCacheModeTest extends AbstractTest {

    public static final String INSERT_POST = "INSERT INTO post (id, title, status) VALUES (?, ?, ?)";

    private static ThreadLocalRandom random = ThreadLocalRandom.current();

    @Override
    protected Class<?>[] entities() {
        return new Class[] {
            Post.class
        };
    }

    @Override
    protected Database database() {
        return Database.POSTGRESQL;
    }

    @Override
    protected void beforeInit() {
        executeStatement("DROP TYPE IF EXISTS post_status");
        executeStatement("CREATE TYPE post_status AS ENUM ('PENDING', 'APPROVED', 'SPAM')");
    }

    @Override
    protected void afterInit() {
        AtomicInteger statementCount = new AtomicInteger();
        long startNanos = System.nanoTime();
        doInJDBC(connection -> {
            try (PreparedStatement statement = connection.prepareStatement(INSERT_POST)) {
                int postCount = getPostCount();

                for (int i = 1; i <= postCount; i++) {
                    PostStatus status = PostStatus.APPROVED;
                    if (i > postCount * 0.99) {
                        status = PostStatus.SPAM;
                    } else if (i > postCount * 0.95) {
                        status = PostStatus.PENDING;
                    }
                    statement.setLong(1, i);
                    statement.setString(2, String.format("High-Performance Java Persistence, page %d", i));
                    statement.setObject(3, PostgreSQLQueries.toEnum(status, "post_status"), Types.OTHER);

                    addToBatch(statement, statementCount);
                }
                statement.executeBatch();
            } catch (SQLException e) {
                fail(e.getMessage());
            }
        });
        LOGGER.info("{}.testInsert took {} millis",
            getClass().getSimpleName(),
            TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - startNanos)
        );
    }

    @Test
    public void testIndexSelectivity() {
        executeStatement(
            "DROP INDEX IF EXISTS idx_post_status",
            """
            CREATE INDEX IF NOT EXISTS idx_post_status ON post (status)
            """,
            "ANALYZE VERBOSE"
        );

        doInJDBC(connection -> {
            executeStatement(
                connection,
                "LOAD 'auto_explain'",
                "SET auto_explain.log_analyze=true",
                "SET auto_explain.log_min_duration=0"
            );
            String planCacheMode = selectColumn(connection, "SHOW plan_cache_mode", String.class);
            LOGGER.info("Plan cache mode: {}", planCacheMode);
            try (PreparedStatement statement = connection.prepareStatement("""
                    SELECT id, title, status
                    FROM post
                    WHERE status = ?
                    """)) {
                executeStatementWithStatus(statement, PostStatus.PENDING);
                executeStatementWithStatus(statement, PostStatus.SPAM);
                executeStatementWithStatus(statement, PostStatus.APPROVED);
            }
        });
    }

    @Test
    public void testDefaultGenericPlanCaching() {
        executeStatement(
            "DROP INDEX IF EXISTS idx_post_status",
            """
            CREATE INDEX IF NOT EXISTS idx_post_status ON post (status) WHERE status != 'APPROVED'
            """,
            "ANALYZE VERBOSE"
        );

        doInJDBC(connection -> {
            executeStatement(
                connection,
                "LOAD 'auto_explain'",
                "SET auto_explain.log_analyze=true",
                "SET auto_explain.log_min_duration=0"
            );
            LOGGER.info(
                "Plan cache mode: {}",
                selectColumn(
                    connection,
                    "SHOW plan_cache_mode",
                    String.class
                )
            );
            try (PreparedStatement statement = connection.prepareStatement("""
                    SELECT id, title, status
                    FROM post
                    WHERE status = ?
                    """)) {
                for (int i = 1; i <= 10; i++) {
                    executeStatementWithStatus(statement, PostStatus.APPROVED);
                }
                executeStatementWithStatus(statement, PostStatus.SPAM);
            }
        });
    }

    @Test
    public void testForceCustomPlanCacheMode() {
        executeStatement(
            "DROP INDEX IF EXISTS idx_post_status",
            """
            CREATE INDEX IF NOT EXISTS idx_post_status ON post (status) WHERE status != 'APPROVED'
            """,
            "ANALYZE VERBOSE"
        );

        doInJDBC(connection -> {
            executeStatement(
                connection,
                "LOAD 'auto_explain'",
                "SET auto_explain.log_analyze=true",
                "SET auto_explain.log_min_duration=0"
            );

            try (PreparedStatement statement = connection.prepareStatement("""
                    SELECT id, title, status
                    FROM post
                    WHERE status = ?
                    """)) {
                for (int i = 1; i <= 10; i++) {
                    executeStatementWithStatus(statement, PostStatus.APPROVED);
                }
                executeStatementWithStatus(statement, PostStatus.SPAM);
                executeStatement(
                    connection,
                    "SET plan_cache_mode=force_custom_plan"
                );
                executeStatementWithStatus(statement, PostStatus.SPAM);
            }
        });
    }

    protected int executeStatementWithStatus(PreparedStatement statement, PostStatus status)
            throws SQLException {
        LOGGER.info(
            "Statement is {}prepared on the server",
            PostgreSQLQueries.isUseServerPrepare(statement) ? "" :
                "not "
        );
        int rowCount = 0;
        statement.setObject(
            1,
            PostgreSQLQueries.toEnum(status, "post_status"),
            Types.OTHER
        );
        try(ResultSet resultSet = statement.executeQuery()) {
            while (resultSet.next()) {
                rowCount++;
            }
        }
        return rowCount;
    }

    private void addToBatch(PreparedStatement statement, AtomicInteger statementCount) throws SQLException {
        statement.addBatch();
        int count = statementCount.incrementAndGet();
        if(count % getBatchSize() == 0) {
            statement.executeBatch();
        }
    }

    protected int getPostCount() {
        return 100_000;
    }

    protected int getBatchSize() {
        return 1000;
    }

    @Entity(name = "Post")
    @Table(name = "post")
    public static class Post {

        @Id
        private Long id;

        @Column(length = 100)
        private String title;

        @Column(columnDefinition = "post_status")
        @JdbcType(PostgreSQLEnumJdbcType.class)
        private PostStatus status;

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

        public PostStatus getStatus() {
            return status;
        }

        public void setStatus(PostStatus status) {
            this.status = status;
        }
    }

    public enum PostStatus {
        PENDING,
        APPROVED,
        SPAM
    }
}
