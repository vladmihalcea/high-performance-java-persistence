package com.vladmihalcea.book.high_performance_java_persistence.jdbc.batch;

import java.sql.PreparedStatement;
import java.sql.SQLException;

/**
 * NoBatchPreparedStatementTest - Test without batching PreparedStatements
 *
 * @author Vlad Mihalcea
 */
public class NoBatchPreparedStatementTest extends AbstractBatchPreparedStatementTest {

    public NoBatchPreparedStatementTest(DataSourceProvider dataSourceProvider) {
        super(dataSourceProvider);
    }

    @Override
    protected void onStatement(PreparedStatement statement) throws SQLException {
       statement.executeUpdate();
    }

    @Override
    protected void onEnd(PreparedStatement statement) throws SQLException {
    }

    @Override
    protected void onFlush(PreparedStatement statement) throws SQLException {

    }
}
