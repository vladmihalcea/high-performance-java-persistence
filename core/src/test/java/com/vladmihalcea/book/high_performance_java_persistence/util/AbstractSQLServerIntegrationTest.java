package com.vladmihalcea.book.high_performance_java_persistence.util;

/**
 * AbstractSQLServerIntegrationTest - Abstract SQL Server IntegrationTest
 *
 * @author Vlad Mihalcea
 */
public abstract class AbstractSQLServerIntegrationTest extends AbstractTest {

    @Override
    protected DataSourceProvider getDataSourceProvider() {
        return new SQLServerDataSourceProvider();
    }
}
