package com.vladmihalcea.book.high_performance_java_persistence.util;

/**
 * AbstractMySQLIntegrationTest - Abstract MySQL IntegrationTest
 *
 * @author Vlad Mihalcea
 */
public abstract class AbstractMySQLIntegrationTest extends AbstractTest {

    @Override
    protected DataSourceProvider getDataSourceProvider() {
        return new MySQLDataSourceProvider();
    }
}
