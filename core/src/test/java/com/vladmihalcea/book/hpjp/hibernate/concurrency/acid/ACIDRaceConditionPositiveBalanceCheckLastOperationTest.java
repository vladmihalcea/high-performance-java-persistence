package com.vladmihalcea.book.hpjp.hibernate.concurrency.acid;

import com.vladmihalcea.book.hpjp.util.AbstractTest;
import com.vladmihalcea.book.hpjp.util.providers.Database;
import org.junit.Test;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.concurrent.CountDownLatch;

import static org.junit.Assert.assertEquals;

/**
 * @author Vlad Mihalcea
 */
public class ACIDRaceConditionPositiveBalanceCheckLastOperationTest extends AbstractTest {

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
            from.setIban("Alice-123");
            from.setOwner("Alice");
            from.setBalance(10);

            entityManager.persist(from);

            Account to = new Account();
            to.setIban("Bob-456");
            to.setOwner("Bob");
            to.setBalance(0L);

            entityManager.persist(to);
        });
    }

    @Test
    public void testSerialExecution() {
        assertEquals(10L, getBalance("Alice-123"));
        assertEquals(0L, getBalance("Bob-456"));

        transfer("Alice-123", "Bob-456", 5L);

        assertEquals(5L, getBalance("Alice-123"));
        assertEquals(5L, getBalance("Bob-456"));

        transfer("Alice-123", "Bob-456", 5L);

        assertEquals(0L, getBalance("Alice-123"));
        assertEquals(10L, getBalance("Bob-456"));

        transfer("Alice-123", "Bob-456", 5L);

        assertEquals(0L, getBalance("Alice-123"));
        assertEquals(10L, getBalance("Bob-456"));
    }

    int threadCount = 8;

    @Test
    public void testParallelExecution() {
        assertEquals(10L, getBalance("Alice-123"));
        assertEquals(0L, getBalance("Bob-456"));

        parallelExecution();

        LOGGER.info("Alice's balance {}", getBalance("Alice-123"));
        LOGGER.info("Bob's balance {}", getBalance("Bob-456"));
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
        long fromBalance = getBalance(fromIban);

        if(fromBalance >= transferCents) {
            addBalance(toIban, transferCents);

            addBalance(fromIban, (-1) * transferCents);
        }
    }

    private long getBalance(final String iban) {
        return doInJDBC(connection -> {
            try(PreparedStatement statement = connection.prepareStatement("""
                    SELECT balance
                    FROM account
                    WHERE iban = ? 
                    """)
            ) {
                statement.setString(1, iban);
                ResultSet resultSet = statement.executeQuery();
                if(resultSet.next()) {
                    return resultSet.getLong(1);
                }
            }
            throw new IllegalArgumentException("Can't find account with IBAN: " + iban);
        });
    }

    private void addBalance(final String iban, long balance) {
        doInJDBC(connection -> {
            try(PreparedStatement statement = connection.prepareStatement("""
                    UPDATE account
                    SET balance = balance + ?
                    WHERE iban = ?
                    """)
            ) {
                statement.setLong(1, balance);
                statement.setString(2, iban);

                statement.executeUpdate();
            }
        });
    }

    @Entity(name = "Account")
    @Table(name = "account")
    public static class Account {

        @Id
        private String iban;

        private String owner;

        private volatile long balance;

        public String getIban() {
            return iban;
        }

        public void setIban(String iban) {
            this.iban = iban;
        }

        public String getOwner() {
            return owner;
        }

        public void setOwner(String owner) {
            this.owner = owner;
        }

        public long getBalance() {
            return balance;
        }

        public void setBalance(long balance) {
            this.balance = balance;
        }
    }
}
