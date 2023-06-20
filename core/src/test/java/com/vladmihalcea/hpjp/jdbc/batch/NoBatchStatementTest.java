package com.vladmihalcea.hpjp.jdbc.batch;

import com.vladmihalcea.hpjp.util.providers.Database;

import java.sql.SQLException;
import java.sql.Statement;

/**
 * BatchStatementTest - Test without batching Statements
 *
 * @author Vlad Mihalcea
 */
public class NoBatchStatementTest extends AbstractBatchStatementTest {

    private int count;

    public NoBatchStatementTest(Database database) {
        super(database);
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