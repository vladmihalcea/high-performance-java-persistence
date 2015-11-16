package com.vladmihalcea.book.high_performance_java_persistence.jdbc.batch;

import com.vladmihalcea.book.high_performance_java_persistence.util.AbstractTest;

import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Statement;

import static org.junit.Assert.assertEquals;

/**
 * BatchPreparedStatementTest - Test batching with PreparedStatements
 *
 * @author Vlad Mihalcea
 */
public class BatchPreparedStatementTest extends AbstractBatchPreparedStatementTest {

    public BatchPreparedStatementTest(DataSourceProvider dataSourceProvider) {
        super(dataSourceProvider);
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
        return 100 * 10 ;
    }
}
