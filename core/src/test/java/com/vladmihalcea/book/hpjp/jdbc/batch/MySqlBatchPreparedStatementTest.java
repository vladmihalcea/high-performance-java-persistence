package com.vladmihalcea.book.hpjp.jdbc.batch;

import com.vladmihalcea.book.hpjp.util.AbstractMySQLIntegrationTest;
import com.vladmihalcea.book.hpjp.util.providers.entity.BlogEntityProvider;
import com.vladmihalcea.book.hpjp.util.providers.DataSourceProvider;
import com.vladmihalcea.book.hpjp.util.providers.MySQLDataSourceProvider;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import java.sql.PreparedStatement;
import java.sql.SQLException;
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
public class MySqlBatchPreparedStatementTest extends AbstractMySQLIntegrationTest {

    private final BlogEntityProvider entityProvider = new BlogEntityProvider();

    private boolean cachePrepStmts;

    private boolean useServerPrepStmts;

    public MySqlBatchPreparedStatementTest(boolean cachePrepStmts, boolean useServerPrepStmts) {
        this.cachePrepStmts = cachePrepStmts;
        this.useServerPrepStmts = useServerPrepStmts;
    }

    @Parameterized.Parameters
    public static Collection<Boolean[]> rdbmsDataSourceProvider() {
        List<Boolean[]> providers = new ArrayList<>();
        providers.add(new Boolean[]{Boolean.FALSE, Boolean.FALSE});
        /*providers.add(new Boolean[]{Boolean.FALSE, Boolean.TRUE});
        providers.add(new Boolean[]{Boolean.TRUE, Boolean.FALSE});
        providers.add(new Boolean[]{Boolean.TRUE, Boolean.TRUE});*/
        return providers;
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
        if(count % getBatchSize() == 0) {
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
