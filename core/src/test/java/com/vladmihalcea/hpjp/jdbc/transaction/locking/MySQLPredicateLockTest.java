package com.vladmihalcea.hpjp.jdbc.transaction.locking;

import com.vladmihalcea.hpjp.util.providers.DataSourceProvider;
import com.vladmihalcea.hpjp.util.providers.MySQLDataSourceProvider;

import java.sql.Connection;
import java.sql.SQLException;

import static org.junit.Assert.fail;

/**
 * @author Vlad Mihalcea
 */
public class MySQLPredicateLockTest extends AbstractPredicateLockTest {

    @Override
    protected DataSourceProvider dataSourceProvider() {
        return new MySQLDataSourceProvider();
    }

    protected void prepareConnection(Connection connection) {
        try {
            connection.setTransactionIsolation(Connection.TRANSACTION_READ_COMMITTED);
        } catch (SQLException e) {
            fail(e.getMessage());
        }
        executeStatement(connection, "SET GLOBAL innodb_lock_wait_timeout = 1");
    }
}
