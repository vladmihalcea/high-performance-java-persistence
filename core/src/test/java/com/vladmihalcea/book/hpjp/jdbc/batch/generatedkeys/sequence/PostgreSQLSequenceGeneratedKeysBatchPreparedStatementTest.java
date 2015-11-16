package com.vladmihalcea.book.hpjp.jdbc.batch.generatedkeys.sequence;

/**
 * PostgreSQLSequenceGeneratedKeysBatchPreparedStatementTest - PostgreSQL class for testing JDBC PreparedStatement generated keys for Sequences
 *
 * @author Vlad Mihalcea
 */
public class PostgreSQLSequenceGeneratedKeysBatchPreparedStatementTest extends AbstractSequenceGeneratedKeysBatchPreparedStatementTest {

    @Override
    protected String callSequenceSyntax() {
        return "select nextval('post_seq')";
    }

    @Override
    protected DataSourceProvider getDataSourceProvider() {
        return new PostgreSQLDataSourceProvider();
    }
}