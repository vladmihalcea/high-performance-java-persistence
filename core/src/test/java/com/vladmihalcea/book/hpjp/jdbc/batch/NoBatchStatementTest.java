package com.vladmihalcea.book.hpjp.jdbc.batch;

import java.sql.SQLException;
import java.sql.Statement;

import com.vladmihalcea.book.hpjp.util.providers.DataSourceProvider;

/**
 * BatchStatementTest - Test without batching Statements
 *
 * @author Vlad Mihalcea
 */
public class NoBatchStatementTest extends AbstractBatchStatementTest {

    private int count;

    public NoBatchStatementTest(DataSourceProvider dataSourceProvider) {
        super(dataSourceProvider);
    }

    @Override
    protected void onStatement(Statement statement, String dml) throws SQLException {
        statement.executeUpdate(dml);
        count++;
    }

    @Override
    protected void onEnd(Statement statement) throws SQLException {
        //assertEquals((getPostCommentCount() + 1) * getPostCount(), count);
    }

    @Override
    protected void onFlush(Statement statement) {

    }
}