package com.vladmihalcea.hpjp.hibernate.concurrency.acid.check;

import com.vladmihalcea.hpjp.hibernate.concurrency.acid.Account;
import com.vladmihalcea.hpjp.util.AbstractTest;
import com.vladmihalcea.hpjp.util.providers.Database;
import org.junit.Test;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.concurrent.CountDownLatch;

import static org.junit.Assert.assertEquals;

/**
 * @author Vlad Mihalcea
 */
public class ACIDRaceConditionPositiveBalanceCheckFirstOperationTest extends AbstractTest {

    @Override
    protected Class<?>[] entities() {
        return new Class<?>[]{
            Account.class
        };
    }

    @Override
    protected Database database() {
        return Database.POSTGRESQL;
    }

    @Override
    protected void afterInit() {
        executeStatement("""
            ALTER TABLE account
            ADD CONSTRAINT account_balance_check
            CHECK (balance >= 0)
            """
        );

        doInJPA(entityManager -> {
            Account from = new Account();
            from.setId("Alice-123");
            from.setOwner("Alice");
            from.setBalance(10);

            entityManager.persist(from);

            Account to = new Account();
            to.setId("Bob-456");
            to.setOwner("Bob");
            to.setBalance(0L);

            entityManager.persist(to);
        });
    }

    @Test
    public void testSerialExecution() {
        assertEquals(10L, getAccountBalance("Alice-123"));
        assertEquals(0L, getAccountBalance("Bob-456"));

        transfer("Alice-123", "Bob-456", 5L);

        assertEquals(5L, getAccountBalance("Alice-123"));
        assertEquals(5L, getAccountBalance("Bob-456"));

        transfer("Alice-123", "Bob-456", 5L);

        assertEquals(0L, getAccountBalance("Alice-123"));
        assertEquals(10L, getAccountBalance("Bob-456"));

        transfer("Alice-123", "Bob-456", 5L);

        assertEquals(0L, getAccountBalance("Alice-123"));
        assertEquals(10L, getAccountBalance("Bob-456"));
    }

    int threadCount = 8;

    @Test
    public void testParallelExecution() {
        assertEquals(10L, getAccountBalance("Alice-123"));
        assertEquals(0L, getAccountBalance("Bob-456"));

        parallelExecution();

        LOGGER.info("Alice's balance {}", getAccountBalance("Alice-123"));
        LOGGER.info("Bob's balance {}", getAccountBalance("Bob-456"));
    }

    public void parallelExecution() {
        CountDownLatch startLatch = new CountDownLatch(1);
        CountDownLatch endLatch = new CountDownLatch(threadCount);

        for (int i = 0; i < threadCount; i++) {
            new Thread(() -> {
                awaitOnLatch(startLatch);
                try {
                    transfer("Alice-123", "Bob-456", 5L);
                } finally {
                    endLatch.countDown();
                }
            }).start();
        }

        startLatch.countDown();
        awaitOnLatch(endLatch);
    }

    public void transfer(String fromIban, String toIban, long transferCents) {
        long fromBalance = getAccountBalance(fromIban);

        if(fromBalance >= transferCents) {
            addToAccountBalance(fromIban, (-1) * transferCents);

            addToAccountBalance(toIban, transferCents);
        }
    }

    private long getAccountBalance(final String id) {
        return doInJDBC(connection -> {
            try(PreparedStatement statement = connection.prepareStatement("""
                    SELECT balance
                    FROM account
                    WHERE id = ? 
                    """)
            ) {
                statement.setString(1, id);
                ResultSet resultSet = statement.executeQuery();
                if(resultSet.next()) {
                    return resultSet.getLong(1);
                }
            }
            throw new IllegalArgumentException("Can't find account with id: " + id);
        });
    }

    private void addToAccountBalance(final String id, long amount) {
        doInJDBC(connection -> {
            try(PreparedStatement statement = connection.prepareStatement("""
                    UPDATE account
                    SET balance = balance + ?
                    WHERE id = ?
                    """)
            ) {
                statement.setLong(1, amount);
                statement.setString(2, id);

                statement.executeUpdate();
            }
        });
    }
}
