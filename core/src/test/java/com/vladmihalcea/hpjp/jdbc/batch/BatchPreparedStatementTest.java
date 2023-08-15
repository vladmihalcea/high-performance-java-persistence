package com.vladmihalcea.hpjp.jdbc.batch;

import com.vladmihalcea.hpjp.util.providers.Database;

import java.sql.PreparedStatement;
import java.sql.SQLException;

/**
 * BatchPreparedStatementTest - Test batching with PreparedStatements
 *
 * @author Vlad Mihalcea
 */
public class BatchPreparedStatementTest extends AbstractBatchPreparedStatementTest {

    public BatchPreparedStatementTest(Database database) {
        super(database);
    }

    @Override
    protected void onStatement(PreparedStatement statement) throws SQLException {
        statement.addBatch();
    }

    @Override
    protected void onEnd(PreparedStatement statement) throws SQLException {
        int[] updateCount = statement.executeBatch();
        statement.clearBatch();
    }

    @Override
    protected void onFlush(PreparedStatement statement) throws SQLException {
        statement.executeBatch();
    }

    @Override
    protected int getBatchSize() {
        return 100 * 10;
    }
}
