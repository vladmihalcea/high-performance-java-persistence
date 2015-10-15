package com.vladmihalcea.book.high_performance_java_persistence.jdbc.batch.generatedkeys.sequence;

/**
 * OracleSequenceCallTest - Oracle sequence call
 *
 * @author Vlad Mihalcea
 */
public class OracleSequenceCallTest extends AbstractSequenceCallTest {

    @Override
    protected String callSequenceSyntax() {
        return "select post_seq.NEXTVAL from dual";
    }

    @Override
    protected DataSourceProvider getDataSourceProvider() {
        return new OracleDataSourceProvider();
    }
}
