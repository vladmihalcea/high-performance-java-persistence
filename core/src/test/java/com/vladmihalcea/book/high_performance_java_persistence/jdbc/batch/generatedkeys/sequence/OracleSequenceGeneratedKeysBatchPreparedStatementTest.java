package com.vladmihalcea.book.high_performance_java_persistence.jdbc.batch.generatedkeys.sequence;

/**
 * OracleSequenceGeneratedKeysBatchPreparedStatementTest - Oracle class for testing JDBC PreparedStatement generated keys for Sequences
 *
 * @author Vlad Mihalcea
 */
public class OracleSequenceGeneratedKeysBatchPreparedStatementTest extends AbstractSequenceGeneratedKeysBatchPreparedStatementTest {

    @Override
    protected String callSequenceSyntax() {
        return "select post_seq.NEXTVAL from dual";
    }

    @Override
    protected DataSourceProvider getDataSourceProvider() {
        return new OracleDataSourceProvider();
    }
}
