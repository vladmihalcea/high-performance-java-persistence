package com.vladmihalcea.book.hpjp.jdbc.batch;

import com.vladmihalcea.book.hpjp.util.AbstractMySQLIntegrationTest;
import com.vladmihalcea.book.hpjp.util.providers.entity.BlogEntityProvider;
import com.vladmihalcea.book.hpjp.util.providers.DataSourceProvider;
import com.vladmihalcea.book.hpjp.util.providers.MySQLDataSourceProvider;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.Assert.fail;

/**
 * MySqlBatchStatementTest - Test MySQl JDBC Statement batching with and w/o rewriteBatchedStatements
 *
 * @author Vlad Mihalcea
 */
@RunWith(Parameterized.class)
public class MySqlBatchStatementTest extends AbstractMySQLIntegrationTest {

    public static final String INSERT_POST = "insert into post (title, version, id) values ('Post no. %1$d', 0, %1$d)";

    public static final String INSERT_POST_COMMENT = "insert into post_comment (post_id, review, version, id) values (%1$d, 'Post comment %2$d', 0, %2$d)";

    private final BlogEntityProvider entityProvider = new BlogEntityProvider();

    private boolean rewriteBatchedStatements;

    public MySqlBatchStatementTest(boolean rewriteBatchedStatements) {
        this.rewriteBatchedStatements = rewriteBatchedStatements;
    }

    @Parameterized.Parameters
    public static Collection<Boolean[]> rdbmsDataSourceProvider() {
        List<Boolean[]> providers = new ArrayList<>();
        providers.add(new Boolean[]{Boolean.FALSE});
        providers.add(new Boolean[]{Boolean.TRUE});
        return providers;
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
        if(count % getBatchSize() == 0) {
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
