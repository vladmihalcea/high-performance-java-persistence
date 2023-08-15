package com.vladmihalcea.hpjp.jdbc.batch;

import com.vladmihalcea.hpjp.util.providers.Database;

import java.sql.SQLException;
import java.sql.Statement;

/**
 * BatchStatementTest - Test batching with Statements
 *
 * @author Vlad Mihalcea
 */
public class BatchStatementTest extends AbstractBatchStatementTest {

    public BatchStatementTest(Database database) {
        super(database);
    }

    @Override
    protected void onStatement(Statement statement, String dml) throws SQLException {
        statement.addBatch(dml);
    }

    @Override
    protected void onEnd(Statement statement) throws SQLException {
        int[] updateCount = statement.executeBatch();
        statement.clearBatch();
    }

    @Override
    protected void onFlush(Statement statement) throws SQLException {
        statement.executeBatch();
    }

    @Override
    protected int getBatchSize() {
        return 100;
    }
}
