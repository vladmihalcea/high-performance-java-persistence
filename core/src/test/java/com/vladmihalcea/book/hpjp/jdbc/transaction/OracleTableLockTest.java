package com.vladmihalcea.book.hpjp.jdbc.transaction;

/**
 * @author Vlad Mihalcea
 */
public class OracleTableLockTest extends AbstractTableLockTest {

    @Override
    protected DataSourceProvider dataSourceProvider() {
        return new OracleDataSourceProvider();
    }

    @Override
    protected String lockEmployeeTableSql() {
        return "LOCK TABLE employee IN SHARE MODE NOWAIT";
    }
}
