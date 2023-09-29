package com.vladmihalcea.hpjp.hibernate.concurrency.acid;

import com.vladmihalcea.hpjp.util.AbstractTest;
import com.vladmihalcea.hpjp.util.providers.Database;
import com.vladmihalcea.hpjp.util.transaction.ConnectionCallable;
import com.vladmihalcea.hpjp.util.transaction.ConnectionVoidCallable;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import org.junit.Test;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.concurrent.CountDownLatch;

import static org.junit.Assert.assertEquals;

/**
 * @author Vlad Mihalcea
 */
public class ACIDRaceConditionTest extends AbstractTest {

    @Override
    protected Class<?>[] entities() {
        return new Class<?>[]{
            Account.class
        };
    }

    @Override
    protected Database database() {
        return Database.MYSQL;
    }

    protected boolean connectionPooling() {
        return true;
    }

    @Override
    protected void afterInit() {
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

    public void transfer(String fromIban, String toIban, long transferCents) {
        long fromBalance = getAccountBalance(fromIban);

        if(fromBalance >= transferCents) {
            addToAccountBalance(fromIban, (-1) * transferCents);

            addToAccountBalance(toIban, transferCents);
        }
    }

    private long getAccountBalance(final String iban) {
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

    private void addToAccountBalance(final String iban, long amount) {
        doInJDBC(connection -> {
            try(PreparedStatement statement = connection.prepareStatement("""
                    UPDATE account
                    SET balance = balance + ?
                    WHERE iban = ?
                    """)
            ) {
                statement.setLong(1, amount);
                statement.setString(2, iban);

                statement.executeUpdate();
            }
        });
    }

    protected void doInJDBC(ConnectionVoidCallable callable) {
        try {
            Connection connection = null;
            try {
                connection = dataSource().getConnection();
                connection.setAutoCommit(false);
                callable.execute(connection);
                connection.commit();
            } catch (SQLException e) {
                if(connection != null) {
                    connection.rollback();
                }
                throw e;
            } finally {
                if(connection !=  null) {
                    connection.close();
                }
            }
        } catch (SQLException e) {
            throw new IllegalStateException(e);
        }
    }

    protected <T> T doInJDBC(ConnectionCallable<T> callable) {
        try {
            Connection connection = null;
            try {
                connection = dataSource().getConnection();
                connection.setAutoCommit(false);
                T result = callable.execute(connection);
                connection.commit();
                return result;
            } catch (SQLException e) {
                if(connection != null) {
                    connection.rollback();
                }
                throw e;
            } finally {
                if(connection !=  null) {
                    connection.close();
                }
            }
        } catch (SQLException e) {
            throw new IllegalStateException(e);
        }
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

    @Test
    public void testParallelExecution() {
        assertEquals(10L, getAccountBalance("Alice-123"));
        assertEquals(0L, getAccountBalance("Bob-456"));

        int threadCount = 16;

        CountDownLatch startLatch = new CountDownLatch(1);
        CountDownLatch endLatch = new CountDownLatch(threadCount);

        for (int i = 0; i < threadCount; i++) {
            new Thread(() -> {
                awaitOnLatch(startLatch);

                transfer("Alice-123", "Bob-456", 5L);

                endLatch.countDown();
            }).start();
        }
        LOGGER.info("Starting threads");
        startLatch.countDown();
        awaitOnLatch(endLatch);

        LOGGER.info("Alice's balance: {}", getAccountBalance("Alice-123"));
        LOGGER.info("Bob's balance: {}", getAccountBalance("Bob-456"));
    }

    @Entity(name = "Account")
    @Table(name = "account")
    public static class Account {

        @Id
        private String iban;

        private String owner;

        private long balance;

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

        public long getAccountBalance() {
            return balance;
        }

        public void setBalance(long balance) {
            this.balance = balance;
        }
    }
}
