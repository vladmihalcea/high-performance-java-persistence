package com.vladmihalcea.book.hpjp.jdbc.transaction.locking;

import java.sql.Connection;

import com.vladmihalcea.book.hpjp.jdbc.transaction.locking.AbstractTableLockTest;
import com.vladmihalcea.book.hpjp.util.providers.DataSourceProvider;
import com.vladmihalcea.book.hpjp.util.providers.MySQLDataSourceProvider;

/**
 * @author Vlad Mihalcea
 */
public class MySQLTableLockTest extends AbstractTableLockTest {

    @Override
    protected DataSourceProvider dataSourceProvider() {
        return new MySQLDataSourceProvider();
    }

    @Override
    protected String lockEmployeeTableSql() {
        return "SELECT * FROM employee WHERE department_id = 1 FOR UPDATE";
    }

    @Override
    protected void prepareConnection(Connection connection) {
        /*try {
            connection.setTransactionIsolation(Connection.TRANSACTION_READ_COMMITTED);
        } catch (SQLException e) {
            fail(e.getMessage());
        }*/
        executeStatement(connection, "SET GLOBAL innodb_lock_wait_timeout = 1");
    }
}
