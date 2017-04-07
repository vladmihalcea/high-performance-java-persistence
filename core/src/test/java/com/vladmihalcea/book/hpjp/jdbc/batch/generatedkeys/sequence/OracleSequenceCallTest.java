package com.vladmihalcea.book.hpjp.jdbc.batch.generatedkeys.sequence;

import com.vladmihalcea.book.hpjp.util.providers.DataSourceProvider;
import com.vladmihalcea.book.hpjp.util.providers.OracleDataSourceProvider;

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
    protected DataSourceProvider dataSourceProvider() {
        return new OracleDataSourceProvider();
    }
}
