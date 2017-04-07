package com.vladmihalcea.book.hpjp.jdbc.batch;

import java.sql.PreparedStatement;
import java.sql.SQLException;

import com.vladmihalcea.book.hpjp.util.providers.DataSourceProvider;

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
