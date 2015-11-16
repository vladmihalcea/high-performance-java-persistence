package com.vladmihalcea.book.hpjp.jdbc.batch.generatedkeys.sequence;

/**
 * PostgreSQLSequenceCallTest - PostgreSQL sequence call
 *
 * @author Vlad Mihalcea
 */
public class SQLServerSequenceCallTest extends AbstractSequenceCallTest {

    @Override
    protected String callSequenceSyntax() {
        return "select NEXT VALUE FOR post_seq";
    }

    @Override
    protected DataSourceProvider getDataSourceProvider() {
        return new SQLServerDataSourceProvider();
    }
}
