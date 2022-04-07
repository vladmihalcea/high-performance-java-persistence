package com.vladmihalcea.book.hpjp.hibernate.concurrency.deadlock.fk;

import org.hibernate.Session;

import jakarta.persistence.EntityManager;
import java.sql.Connection;

/**
 * @author Vlad Mihalcea
 */
public class MySQLFKNoParentLockSerializableTest extends MySQLFKNoParentLockRRTest {

    protected void prepareConnection(EntityManager entityManager) {
        entityManager.unwrap(Session.class).doWork(connection -> {
            connection.setTransactionIsolation(Connection.TRANSACTION_SERIALIZABLE);
            setJdbcTimeout(connection);
        });
    }
}
