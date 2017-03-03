package com.vladmihalcea.book.hpjp.jdbc.transaction;

import java.sql.Connection;

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
        executeStatement(connection, "SET GLOBAL innodb_lock_wait_timeout = 100");
    }
}
