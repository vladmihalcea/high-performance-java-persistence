package com.vladmihalcea.book.hpjp.util;

/**
 * AbstractOracleXEIntegrationTest - Abstract Orcale XE IntegrationTest
 *
 * @author Vlad Mihalcea
 */
public abstract class AbstractOracleXEIntegrationTest extends AbstractTest {

    @Override
    protected DataSourceProvider dataSourceProvider() {
        return new OracleDataSourceProvider();
    }
}
