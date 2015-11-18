package com.vladmihalcea.book.hpjp.jdbc.transaction;

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
    protected DataSourceProvider dataSourceProvider() {
        return new MySQLDataSourceProvider();
    }
}
