package com.vladmihalcea.hpjp.jdbc.batch;

import com.vladmihalcea.hpjp.util.providers.Database;

import java.sql.PreparedStatement;
import java.sql.SQLException;

/**
 * NoBatchPreparedStatementTest - Test without batching PreparedStatements
 *
 * @author Vlad Mihalcea
 */
public class NoBatchPreparedStatementTest extends AbstractBatchPreparedStatementTest {

    public NoBatchPreparedStatementTest(Database database) {
        super(database);
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
