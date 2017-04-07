package com.vladmihalcea.book.hpjp.jdbc.batch;

import com.vladmihalcea.book.hpjp.util.DataSourceProviderIntegrationTest;
import com.vladmihalcea.book.hpjp.util.providers.entity.BlogEntityProvider;
import com.vladmihalcea.book.hpjp.util.providers.DataSourceProvider;

import org.junit.Test;

import java.sql.SQLException;
import java.sql.Statement;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.Assert.fail;

/**
 * AbstractBatchStatementTest - Base class for testing JDBC Statement batching
 *
 * @author Vlad Mihalcea
 */
public abstract class AbstractBatchStatementTest extends DataSourceProviderIntegrationTest {

    public static final String INSERT_POST = "insert into post (title, version, id) values ('Post no. %1$d', 0, %1$d)";

    public static final String INSERT_POST_COMMENT = "insert into post_comment (post_id, review, version, id) values (%1$d, 'Post comment %2$d', 0, %2$d)";

    private final BlogEntityProvider entityProvider = new BlogEntityProvider();

    public AbstractBatchStatementTest(DataSourceProvider dataSourceProvider) {
        super(dataSourceProvider);
    }

    @Override
    protected Class<?>[] entities() {
        return entityProvider.entities();
    }

    @Test
    public void testInsert() {
        LOGGER.info("Test batch insert");
        AtomicInteger statementCount = new AtomicInteger();
        long startNanos = System.nanoTime();
        doInJDBC(connection -> {
            try (Statement statement = connection.createStatement()) {
                int postCount = getPostCount();
                int postCommentCount = getPostCommentCount();

                if (mix()) {
                    for (int i = 0; i < postCount; i++) {
                        executeStatement(statement, String.format(INSERT_POST, i), statementCount);
                        for (int j = 0; j < postCommentCount; j++) {
                            executeStatement(statement, String.format(INSERT_POST_COMMENT, i, (postCommentCount * i) + j), statementCount);
                        }
                    }
                    onEnd(statement);
                } else {
                    for (int i = 0; i < postCount; i++) {
                        executeStatement(statement, String.format(INSERT_POST, i), statementCount);
                    }
                    onEnd(statement);

                    for (int i = 0; i < postCount; i++) {
                        for (int j = 0; j < postCommentCount; j++) {
                            executeStatement(statement, String.format(INSERT_POST_COMMENT, i, (postCommentCount * i) + j), statementCount);
                        }
                    }
                    onEnd(statement);
                }
            } catch (SQLException e) {
                fail(e.getMessage());
            }
        });
        LOGGER.info("{}.testInsert for {} took {} millis",
                getClass().getSimpleName(),
                dataSourceProvider().getClass().getSimpleName(),
                TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - startNanos));
    }

    protected abstract void onFlush(Statement statement) throws SQLException;

    private void executeStatement(Statement statement, String dml, AtomicInteger statementCount) throws SQLException {
        onStatement(statement, dml);
        int count = statementCount.incrementAndGet();
        if(count % getBatchSize() == 0) {
            onFlush(statement);
        }
    }

    protected abstract void onStatement(Statement statement, String dml) throws SQLException;

    protected abstract void onEnd(Statement statement) throws SQLException;

    protected int getPostCount() {
        return 1000;
    }

    protected int getPostCommentCount() {
        return 4;
    }

    protected int getBatchSize() {
        return 50;
    }

    protected boolean mix() {
        return false;
    }
}
