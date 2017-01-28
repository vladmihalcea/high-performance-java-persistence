package com.vladmihalcea.book.hpjp.jdbc.transaction;

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
    protected String lockEmployeeTableSql() {
        return "LOCK TABLE employee IN SHARE ROW EXCLUSIVE MODE NOWAIT";
    }

    @Override
    protected DataSourceProvider dataSourceProvider() {
        return new PostgreSQLDataSourceProvider();
    }
}
