package com.vladmihalcea.hpjp.jdbc.batch;

import com.vladmihalcea.hpjp.util.AbstractMySQLIntegrationTest;
import com.vladmihalcea.hpjp.util.providers.DataSourceProvider;
import com.vladmihalcea.hpjp.util.providers.MySQLDataSourceProvider;
import com.vladmihalcea.hpjp.util.providers.entity.BlogEntityProvider;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.Parameter;
import org.junit.jupiter.params.ParameterizedClass;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.sql.SQLException;
import java.sql.Statement;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.fail;

/**
 * MySqlBatchStatementTest - Test MySQl JDBC Statement batching with and w/o rewriteBatchedStatements
 *
 * @author Vlad Mihalcea
 */
@ParameterizedClass
@MethodSource("parameters")
public class MySQLBatchStatementTest extends AbstractMySQLIntegrationTest {

    public static final String INSERT_POST = "insert into post (title, version, id) values ('Post no. %1$d', 0, %1$d)";

    public static final String INSERT_POST_COMMENT = "insert into post_comment (post_id, review, version, id) values (%1$d, 'Post comment %2$d', 0, %2$d)";

    private final BlogEntityProvider entityProvider = new BlogEntityProvider();

    @Parameter
    private boolean rewriteBatchedStatements;

    public static Stream<Arguments> parameters() {
        return Stream.of(
            Arguments.of(Boolean.FALSE),
            Arguments.of(Boolean.TRUE)
        );
    }

    @Override
    protected Class<?>[] entities() {
        return entityProvider.entities();
    }

    @Override
    protected DataSourceProvider dataSourceProvider() {
        MySQLDataSourceProvider dataSourceProvider = (MySQLDataSourceProvider) super.dataSourceProvider();
        dataSourceProvider.setRewriteBatchedStatements(rewriteBatchedStatements);
        return dataSourceProvider;
    }

    @Test
    public void testInsert() {
        if (!ENABLE_LONG_RUNNING_TESTS) {
            return;
        }
        LOGGER.info("Test MySQL batch insert with rewriteBatchedStatements={}", rewriteBatchedStatements);
        AtomicInteger statementCount = new AtomicInteger();
        long startNanos = System.nanoTime();
        doInJDBC(connection -> {
            try (Statement statement = connection.createStatement()) {
                int postCount = getPostCount();
                int postCommentCount = getPostCommentCount();

                for (int i = 0; i < postCount; i++) {
                    executeStatement(statement, String.format(INSERT_POST, i), statementCount);
                }
                statement.executeBatch();

                for (int i = 0; i < postCount; i++) {
                    for (int j = 0; j < postCommentCount; j++) {
                        executeStatement(statement, String.format(INSERT_POST_COMMENT, i, (postCommentCount * i) + j), statementCount);
                    }
                }
                statement.executeBatch();
            } catch (SQLException e) {
                fail(e.getMessage());
            }
        });
        LOGGER.info("{}.testInsert for rewriteBatchedStatements={} took {} millis",
            getClass().getSimpleName(),
            rewriteBatchedStatements,
            TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - startNanos));
    }

    private void executeStatement(Statement statement, String dml, AtomicInteger statementCount) throws SQLException {
        statement.addBatch(dml);
        int count = statementCount.incrementAndGet();
        if (count % getBatchSize() == 0) {
            statement.executeBatch();
        }
    }

    protected int getPostCount() {
        return 1000;
    }

    protected int getPostCommentCount() {
        return 4;
    }

    protected int getBatchSize() {
        return 100 * 10;
    }
}
