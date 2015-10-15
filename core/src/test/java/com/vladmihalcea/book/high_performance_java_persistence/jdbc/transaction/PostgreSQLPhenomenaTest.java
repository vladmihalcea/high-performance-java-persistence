package com.vladmihalcea.book.high_performance_java_persistence.jdbc.transaction;

/**
 * PostgreSQLPhenomenaTest - Test to validate PostgreSQL phenomena
 *
 * @author Vlad Mihalcea
 */
public class PostgreSQLPhenomenaTest extends AbstractPhenomenaTest {

    public PostgreSQLPhenomenaTest(String isolationLevelName, int isolationLevel) {
        super(isolationLevelName, isolationLevel);
    }

    @Override
    protected DataSourceProvider getDataSourceProvider() {
        return new PostgreSQLDataSourceProvider();
    }
}
