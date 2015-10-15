package com.vladmihalcea.book.high_performance_java_persistence.jdbc.batch.generatedkeys.sequence;

/**
 * PostgreSQLSequenceCallTest - PostgreSQL sequence call
 *
 * @author Vlad Mihalcea
 */
public class PostgreSQLSequenceCallTest extends AbstractSequenceCallTest {

    @Override
    protected String callSequenceSyntax() {
        return "select nextval('post_seq')";
    }

    @Override
    protected DataSourceProvider getDataSourceProvider() {
        return new PostgreSQLDataSourceProvider();
    }
}
