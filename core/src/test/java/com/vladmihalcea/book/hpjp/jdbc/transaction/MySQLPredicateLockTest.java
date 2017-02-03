package com.vladmihalcea.book.hpjp.jdbc.transaction;

import java.sql.Connection;

/**
 * @author Vlad Mihalcea
 */
public class MySQLPredicateLockTest extends AbstractPredicateLockTest {

    @Override
    protected DataSourceProvider dataSourceProvider() {
        return new MySQLDataSourceProvider();
    }

    protected void prepareConnection(Connection connection) {
        executeStatement(connection, "SET GLOBAL innodb_lock_wait_timeout = 1");
    }
}
