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
    protected String lockEmployeeTableSql() {
        return "SELECT * FROM employee WHERE department_id = 1 FOR UPDATE";
    }

    @Override
    protected DataSourceProvider dataSourceProvider() {
        return new MySQLDataSourceProvider();
    }
}
