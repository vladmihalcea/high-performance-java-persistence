package com.vladmihalcea.book.hpjp.util;

/**
 * AbstractSQLServerIntegrationTest - Abstract SQL Server IntegrationTest
 *
 * @author Vlad Mihalcea
 */
public abstract class AbstractSQLServerIntegrationTest extends AbstractTest {

    @Override
    protected DataSourceProvider dataSourceProvider() {
        return new SQLServerDataSourceProvider();
    }
}
