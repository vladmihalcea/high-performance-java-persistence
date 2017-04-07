package com.vladmihalcea.book.hpjp.jdbc.batch.generatedkeys.sequence;

import com.vladmihalcea.book.hpjp.util.AbstractTest;
import com.vladmihalcea.book.hpjp.util.providers.entity.SequenceBatchEntityProvider;
import org.junit.Test;

import java.sql.*;
import java.util.concurrent.TimeUnit;

/**
 * AbstractSequenceGeneratedKeysBatchPreparedStatementTest - Base class for testing JDBC PreparedStatement generated keys for Sequences
 *
 * @author Vlad Mihalcea
 */
public abstract class AbstractSequenceGeneratedKeysBatchPreparedStatementTest extends AbstractTest {

    private SequenceBatchEntityProvider entityProvider = new SequenceBatchEntityProvider();

    @Override
    protected Class<?>[] entities() {
        return entityProvider.entities();
    }

    @Test
    public void testBatch() {
        doInJDBC(this::batchInsert);
    }

    protected int getPostCount() {
        return 5 * 1000;
    }

    protected int getBatchSize() {
        return 25;
    }

    protected int getAllocationSize() {
        return 1;
    }

    protected void batchInsert(Connection connection) throws SQLException {
        DatabaseMetaData databaseMetaData = connection.getMetaData();
        LOGGER.info("{} Driver supportsGetGeneratedKeys: {}", dataSourceProvider().database(), databaseMetaData.supportsGetGeneratedKeys());

        dropSequence(connection);
        createSequence(connection);

        long startNanos = System.nanoTime();
        int postCount = getPostCount();
        int batchSize = getBatchSize();
        try(PreparedStatement postStatement = connection.prepareStatement(
                "INSERT INTO post (id, title, version) VALUES (?, ?, ?)")) {
            for (int i = 0; i < postCount; i++) {
                if(i > 0 && i % batchSize == 0) {
                    postStatement.executeBatch();
                }
                postStatement.setLong(1, getNextSequenceValue(connection));
                postStatement.setString(2, String.format("Post no. %1$d", i));
                postStatement.setInt(3, 0);
                postStatement.addBatch();
            }
            postStatement.executeBatch();
        }

        LOGGER.info("{}.testInsert for {} using allocation size {} took {} millis",
                getClass().getSimpleName(),
                dataSourceProvider().getClass().getSimpleName(),
                getAllocationSize(),
                TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - startNanos));
    }

    private long getNextSequenceValue(Connection connection)
        throws SQLException {
        try(Statement statement = connection.createStatement()) {
            try(ResultSet resultSet = statement.executeQuery(
                callSequenceSyntax())) {
                resultSet.next();
                return resultSet.getLong(1);
            }
        }
    }

    protected abstract String callSequenceSyntax();

    protected void dropSequence(Connection connection) {
        try(Statement statement = connection.createStatement()) {
            statement.executeUpdate("drop sequence post_seq");
        } catch (Exception ignore) {}
    }

    protected void createSequence(Connection connection) {
        try(Statement statement = connection.createStatement()) {
            statement.executeUpdate(String.format("create sequence post_seq start with 1 increment by %d", getAllocationSize()));
        } catch (Exception ignore) {}
    }
}
