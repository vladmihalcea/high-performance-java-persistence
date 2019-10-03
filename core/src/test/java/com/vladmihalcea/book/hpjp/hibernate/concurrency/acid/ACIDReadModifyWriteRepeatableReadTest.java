package com.vladmihalcea.book.hpjp.hibernate.concurrency.acid;

import java.sql.Connection;
import java.sql.SQLException;

/**
 * @author Vlad Mihalcea
 */
public class ACIDReadModifyWriteRepeatableReadTest extends ACIDReadModifyWriteDefaultIsolationLevelTest {

    @Override
    protected void setIsolationLevel(Connection connection) throws SQLException {
        connection.setTransactionIsolation(Connection.TRANSACTION_REPEATABLE_READ);
    }
}
