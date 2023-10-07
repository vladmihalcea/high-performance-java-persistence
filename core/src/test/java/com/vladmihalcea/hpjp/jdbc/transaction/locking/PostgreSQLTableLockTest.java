package com.vladmihalcea.hpjp.jdbc.transaction.locking;

import com.vladmihalcea.hpjp.util.providers.DataSourceProvider;
import com.vladmihalcea.hpjp.util.providers.PostgreSQLDataSourceProvider;

import java.sql.Connection;

/**
 * @author Vlad Mihalcea
 */
public class PostgreSQLTableLockTest extends AbstractTableLockTest {

    @Override
    protected DataSourceProvider dataSourceProvider() {
        return new PostgreSQLDataSourceProvider();
    }

    @Override
    protected String lockEmployeeTableSql() {
        return "LOCK TABLE employee IN SHARE ROW EXCLUSIVE MODE";
    }

    @Override
    protected void prepareConnection(Connection connection) {
        executeStatement(connection, "SET statement_timeout TO 1000");
    }
}
