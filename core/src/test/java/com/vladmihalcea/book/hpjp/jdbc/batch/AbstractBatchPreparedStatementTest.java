package com.vladmihalcea.book.hpjp.jdbc.batch;

import com.vladmihalcea.book.hpjp.util.DataSourceProviderIntegrationTest;
import com.vladmihalcea.book.hpjp.util.providers.entity.BlogEntityProvider;
import com.vladmihalcea.book.hpjp.util.providers.DataSourceProvider;

import org.junit.Test;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * AbstractBatchPreparedStatementTest - Base class for testing JDBC PreparedStatement  batching
 *
 * @author Vlad Mihalcea
 */
public abstract class AbstractBatchPreparedStatementTest extends DataSourceProviderIntegrationTest {

    private BlogEntityProvider entityProvider = new BlogEntityProvider();

    public AbstractBatchPreparedStatementTest(DataSourceProvider dataSourceProvider) {
        super(dataSourceProvider);
    }

    @Override
    protected Class<?>[] entities() {
        return entityProvider.entities();
    }

    @Test
    public void testBatch() {
        doInJDBC(connection -> {
            batchInsert(connection);
            batchUpdate(connection);
            batchDelete(connection);
        });
    }

    protected abstract void onFlush(PreparedStatement statement) throws SQLException;

    private void executeStatement(PreparedStatement statement, AtomicInteger statementCount) throws SQLException {
        onStatement(statement);
        int count = statementCount.incrementAndGet();
        if(count % getBatchSize() == 0) {
            onFlush(statement);
        }
    }

    protected abstract void onStatement(PreparedStatement statement) throws SQLException;

    protected abstract void onEnd(PreparedStatement statement) throws SQLException;

    protected int getPostCount() {
        return 1000;
    }

    protected int getPostCommentCount() {
        return 4;
    }

    protected int getBatchSize() {
        return 1;
    }

    protected void batchInsert(Connection connection) throws SQLException {
        AtomicInteger postStatementCount = new AtomicInteger();
        AtomicInteger postCommentStatementCount = new AtomicInteger();

        try (PreparedStatement postStatement = connection.prepareStatement("insert into post (title, version, id) values (?, ?, ?)");
             PreparedStatement postCommentStatement = connection.prepareStatement("insert into post_comment (post_id, review, version, id) values (?, ?, ?, ?)");
        ) {
            int postCount = getPostCount();
            int postCommentCount = getPostCommentCount();

            LOGGER.info("Test batch insert");
            long startNanos = System.nanoTime();

            int index;

            for (int i = 0; i < postCount; i++) {
                index = 0;

                postStatement.setString(++index, String.format("Post no. %1$d", i));
                postStatement.setInt(++index, 0);
                postStatement.setLong(++index, i);
                executeStatement(postStatement, postStatementCount);
            }
            onEnd(postStatement);
            for (int i = 0; i < postCount; i++) {
                for (int j = 0; j < postCommentCount; j++) {
                    index = 0;

                    postCommentStatement.setLong(++index, i);
                    postCommentStatement.setString(++index, String.format("Post comment %1$d", j));
                    postCommentStatement.setInt(++index, 0);
                    postCommentStatement.setLong(++index, (postCommentCount * i) + j);
                    executeStatement(postCommentStatement, postCommentStatementCount);
                }
            }
            onEnd(postCommentStatement);

            LOGGER.info("{}.testInsert for {} using batch size {} took {} millis",
                    getClass().getSimpleName(),
                    dataSourceProvider().getClass().getSimpleName(),
                    getBatchSize(),
                    TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - startNanos));

        }
    }

    protected void batchUpdate(Connection connection) throws SQLException {
        AtomicInteger postStatementCount = new AtomicInteger();
        AtomicInteger postCommentStatementCount = new AtomicInteger();

        try (
             PreparedStatement postStatement = connection.prepareStatement("update Post set version = ? where id = ?");
             PreparedStatement postCommentStatement = connection.prepareStatement("update post_comment set version = ? where id = ?");
             Statement bulkUpdateStatement = connection.createStatement();
        ) {
            int postCount = getPostCount();
            int postCommentCount = getPostCommentCount();

            LOGGER.info("Test batch update");

            long startNanos = System.nanoTime();

            int index;

            for (int i = 0; i < postCount; i++) {
                index = 0;

                postStatement.setInt(++index, 1);
                postStatement.setLong(++index, i);
                executeStatement(postStatement, postStatementCount);
            }
            onEnd(postStatement);
            for (int i = 0; i < postCount; i++) {
                for (int j = 0; j < postCommentCount; j++) {
                    index = 0;

                    postCommentStatement.setInt(++index, 1);
                    postCommentStatement.setLong(++index, (postCommentCount * i) + j);
                    executeStatement(postCommentStatement, postCommentStatementCount);
                }
            }
            onEnd(postCommentStatement);

            LOGGER.info("{}.testUpdate for {} took {} millis",
                    getClass().getSimpleName(),
                    dataSourceProvider().getClass().getSimpleName(),
                    TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - startNanos));

            LOGGER.info("Test bulk update");
            startNanos = System.nanoTime();
            bulkUpdateStatement.executeUpdate("update Post set version = version + 1");
            bulkUpdateStatement.executeUpdate("update post_comment set version = version + 1");
            LOGGER.info("{}.testBulkUpdate for {} took {} millis",
                    getClass().getSimpleName(),
                    dataSourceProvider().getClass().getSimpleName(),
                    TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - startNanos));
        }
    }

    protected void batchDelete(Connection connection) throws SQLException {
        AtomicInteger postStatementCount = new AtomicInteger();
        AtomicInteger postCommentStatementCount = new AtomicInteger();

        try (
                PreparedStatement postStatement = connection.prepareStatement("delete from Post where id = ?");
                PreparedStatement postCommentStatement = connection.prepareStatement("delete from post_comment where id = ?");
                Statement bulkUpdateStatement = connection.createStatement();
        ) {
            int postCount = getPostCount();
            int postCommentCount = getPostCommentCount();

            LOGGER.info("Test batch update");

            long startNanos = System.nanoTime();

            int index;

            for (int i = 0; i < postCount; i++) {
                for (int j = 0; j < postCommentCount; j++) {
                    index = 0;

                    postCommentStatement.setLong(++index, (postCommentCount * i) + j);
                    executeStatement(postCommentStatement, postCommentStatementCount);
                }
            }
            onEnd(postCommentStatement);

            for (int i = 0; i < postCount; i++) {
                index = 0;

                postStatement.setLong(++index, i);
                executeStatement(postStatement, postStatementCount);
            }
            onEnd(postStatement);

            LOGGER.info("{}.testDelete for {} took {} millis",
                    getClass().getSimpleName(),
                    dataSourceProvider().getClass().getSimpleName(),
                    TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - startNanos));

            batchInsert(connection);

            LOGGER.info("Test bulk delete");
            startNanos = System.nanoTime();
            bulkUpdateStatement.executeUpdate("delete from post_comment where version > 0");
            bulkUpdateStatement.executeUpdate("delete from Post where version > 0");
            LOGGER.info("{}.testBulkDelete for {} took {} millis",
                    getClass().getSimpleName(),
                    dataSourceProvider().getClass().getSimpleName(),
                    TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - startNanos));
        }

    }
}
