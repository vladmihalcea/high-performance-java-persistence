package com.vladmihalcea.book.high_performance_java_persistence.jdbc.transaction;

/**
 * MySQLPhenomenaTest - Test to validate MySQL phenomena
 *
 * @author Vlad Mihalcea
 */
public class MySQLPhenomenaTest extends AbstractPhenomenaTest {

    public MySQLPhenomenaTest(String isolationLevelName, int isolationLevel) {
        super(isolationLevelName, isolationLevel);
    }

    @Override
    protected DataSourceProvider getDataSourceProvider() {
        return new MySQLDataSourceProvider();
    }
}
