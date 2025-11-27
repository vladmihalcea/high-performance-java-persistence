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

import java.sql.PreparedStatement;
import java.sql.SQLException;
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
public class MySQLBatchPreparedStatementTest extends AbstractMySQLIntegrationTest {

    private final BlogEntityProvider entityProvider = new BlogEntityProvider();

    @Parameter(0)
    private boolean cachePrepStmts;
    @Parameter(1)
    private boolean useServerPrepStmts;

    public static Stream<Arguments> parameters() {
        return Stream.of(
            Arguments.of(Boolean.FALSE, Boolean.FALSE),
            Arguments.of(Boolean.FALSE, Boolean.TRUE),
            Arguments.of(Boolean.TRUE, Boolean.FALSE),
            Arguments.of(Boolean.TRUE, Boolean.TRUE)
        );
    }

    @Override
    protected Class<?>[] entities() {
        return entityProvider.entities();
    }

    @Override
    protected DataSourceProvider dataSourceProvider() {
        MySQLDataSourceProvider dataSourceProvider = (MySQLDataSourceProvider) super.dataSourceProvider();
        dataSourceProvider.setCachePrepStmts(cachePrepStmts);
        dataSourceProvider.setUseServerPrepStmts(useServerPrepStmts);
        return dataSourceProvider;
    }

    @Test
    public void testInsert() {
        if (!ENABLE_LONG_RUNNING_TESTS) {
            return;
        }
        LOGGER.info("Test MySQL batch insert with cachePrepStmts={}, useServerPrepStmts={}", cachePrepStmts, useServerPrepStmts);
        AtomicInteger statementCount = new AtomicInteger();
        long startNanos = System.nanoTime();
        doInJDBC(connection -> {
            AtomicInteger postStatementCount = new AtomicInteger();
            AtomicInteger postCommentStatementCount = new AtomicInteger();

            try (PreparedStatement postStatement = connection.prepareStatement("insert into post (title, version, id) values (?, ?, ?)");
                 PreparedStatement postCommentStatement = connection.prepareStatement("insert into post_comment (post_id, review, version, id) values (?, ?, ?, ?)");
            ) {
                int postCount = getPostCount();
                int postCommentCount = getPostCommentCount();

                for (int i = 0; i < postCount; i++) {
                    int index = 0;

                    postStatement.setString(++index, String.format("Post no. %1$d", i));
                    postStatement.setInt(++index, 0);
                    postStatement.setLong(++index, i);
                    executeStatement(postStatement, postStatementCount);
                }
                postStatement.executeBatch();

                for (int i = 0; i < postCount; i++) {
                    for (int j = 0; j < postCommentCount; j++) {
                        int index = 0;

                        postCommentStatement.setLong(++index, i);
                        postCommentStatement.setString(++index, String.format("Post comment %1$d", j));
                        postCommentStatement.setInt(++index, 0);
                        postCommentStatement.setLong(++index, (postCommentCount * i) + j);
                        executeStatement(postCommentStatement, postCommentStatementCount);
                    }
                }
                postCommentStatement.executeBatch();
            } catch (SQLException e) {
                fail(e.getMessage());
            }
        });
        LOGGER.info("{}.testInsert for cachePrepStmts={}, useServerPrepStmts={} took {} millis",
            getClass().getSimpleName(),
            cachePrepStmts,
            useServerPrepStmts,
            TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - startNanos));
    }

    private void executeStatement(PreparedStatement statement, AtomicInteger statementCount) throws SQLException {
        statement.addBatch();
        int count = statementCount.incrementAndGet();
        if (count % getBatchSize() == 0) {
            statement.executeBatch();
        }
    }

    protected int getPostCount() {
        return 5000;
    }

    protected int getPostCommentCount() {
        return 1;
    }

    protected int getBatchSize() {
        return 100 * 10;
    }
}
