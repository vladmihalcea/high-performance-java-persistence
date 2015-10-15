package com.vladmihalcea.book.high_performance_java_persistence.jdbc.batch.generatedkeys.sequence;

/**
 * SQLServerSequenceGeneratedKeysBatchPreparedStatementTest - SQL Server class for testing JDBC PreparedStatement generated keys for Sequences
 *
 * @author Vlad Mihalcea
 */
public class SQLServerSequenceGeneratedKeysBatchPreparedStatementTest extends AbstractSequenceGeneratedKeysBatchPreparedStatementTest {

    @Override
    protected String callSequenceSyntax() {
        return "select NEXT VALUE FOR post_seq";
    }

    @Override
    protected DataSourceProvider getDataSourceProvider() {
        return new SQLServerDataSourceProvider();
    }
}