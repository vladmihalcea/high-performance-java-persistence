package com.vladmihalcea.book.high_performance_java_persistence.util;

/**
 * AbstractPostgreSQLIntegrationTest - Abstract PostgreSQL IntegrationTest
 *
 * @author Vlad Mihalcea
 */
public abstract class AbstractPostgreSQLIntegrationTest extends AbstractTest {

    @Override
    protected DataSourceProvider getDataSourceProvider() {
        return new PostgreSQLDataSourceProvider();
    }
}
