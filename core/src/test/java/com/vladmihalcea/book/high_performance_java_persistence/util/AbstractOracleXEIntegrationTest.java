package com.vladmihalcea.book.high_performance_java_persistence.util;

/**
 * AbstractOracleXEIntegrationTest - Abstract Orcale XE IntegrationTest
 *
 * @author Vlad Mihalcea
 */
public abstract class AbstractOracleXEIntegrationTest extends AbstractTest {

    @Override
    protected DataSourceProvider getDataSourceProvider() {
        return new OracleDataSourceProvider();
    }
}
