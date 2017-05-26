package com.vladmihalcea.book.hpjp.jdbc.transaction.locking;

import java.sql.Connection;

import com.vladmihalcea.book.hpjp.jdbc.transaction.locking.AbstractTableLockTest;
import com.vladmihalcea.book.hpjp.util.providers.DataSourceProvider;
import com.vladmihalcea.book.hpjp.util.providers.PostgreSQLDataSourceProvider;

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
        return "LOCK TABLE employee IN SHARE ROW EXCLUSIVE MODE NOWAIT";
    }

    @Override
    protected void prepareConnection(Connection connection) {
        executeStatement(connection, "SET statement_timeout TO 1000");
    }
}
