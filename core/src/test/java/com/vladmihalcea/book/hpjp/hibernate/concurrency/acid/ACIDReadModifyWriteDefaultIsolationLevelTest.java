package com.vladmihalcea.book.hpjp.hibernate.concurrency.acid;

import com.vladmihalcea.book.hpjp.util.AbstractTest;
import com.vladmihalcea.book.hpjp.util.providers.Database;
import org.junit.Test;

import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Table;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.concurrent.CountDownLatch;

import static org.junit.Assert.assertEquals;

/**
 * @author Vlad Mihalcea
 */
public class ACIDReadModifyWriteDefaultIsolationLevelTest extends AbstractTest {

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
    public void testParallelExecution() {
        assertEquals(10L, getBalance("Alice-123"));
        assertEquals(0L, getBalance("Bob-456"));

        parallelExecution();

        LOGGER.info("Alice's balance {}", getBalance("Alice-123"));
        LOGGER.info("Bob's balance {}", getBalance("Bob-456"));
    }

    int threadCount = 16;

    public void parallelExecution() {

        String fromIban = "Alice-123";
        String toIban = "Bob-456";
        Long transferCents = 5L;

        CountDownLatch workerThreadWaitsAfterReadingBalanceLatch = new CountDownLatch(threadCount);
        CountDownLatch workerThreadsWriteBalanceLatch = new CountDownLatch(1);
        CountDownLatch allWorkerThreadsHaveFinishedLatch = new CountDownLatch(threadCount);

        for (int i = 0; i < threadCount; i++) {
            new Thread(() -> {
                doInJDBC(connection -> {
                    printIsolationLevel(connection);

                    Long fromBalance = getBalance(connection, fromIban);

                    workerThreadWaitsAfterReadingBalanceLatch.countDown();
                    awaitOnLatch(workerThreadsWriteBalanceLatch);
                    LOGGER.info("Running thread");

                    if(fromBalance >= transferCents) {
                        addBalance(connection, fromIban, (-1) * transferCents);

                        addBalance(connection, toIban, transferCents);
                    }
                });

                allWorkerThreadsHaveFinishedLatch.countDown();
            }).start();
        }
        LOGGER.info("Starting threads");
        awaitOnLatch(workerThreadWaitsAfterReadingBalanceLatch);
        workerThreadsWriteBalanceLatch.countDown();
        awaitOnLatch(allWorkerThreadsHaveFinishedLatch);
    }

    public void transfer() {

    }

    private void printIsolationLevel(Connection connection) throws SQLException {
        int isolationLevelIntegerValue = connection.getTransactionIsolation();

        String isolationLevelStringValue = null;

        switch (isolationLevelIntegerValue) {
            case Connection.TRANSACTION_READ_UNCOMMITTED:
                isolationLevelStringValue = "READ_UNCOMMITTE";
                break;
            case Connection.TRANSACTION_READ_COMMITTED:
                isolationLevelStringValue = "READ_COMMITTED";
                break;
            case Connection.TRANSACTION_REPEATABLE_READ:
                isolationLevelStringValue = "REPEATABLE_READ";
                break;
            case Connection.TRANSACTION_SERIALIZABLE:
                isolationLevelStringValue = "SERIALIZABLE";
                break;
        }

        LOGGER.info("Transaction isolation level: {}", isolationLevelStringValue);
    }

    private long getBalance(Connection connection, final String iban) {
        try(PreparedStatement statement = connection.prepareStatement(
            "SELECT balance " +
            "FROM account " +
            "WHERE iban = ?")
        ) {
            statement.setString(1, iban);
            ResultSet resultSet = statement.executeQuery();
            if(resultSet.next()) {
                return resultSet.getLong(1);
            }
        } catch (SQLException e) {
            throw new IllegalStateException(e);
        }
        throw new IllegalArgumentException("Can't find account with IBAN: " + iban);
    }

    private long getBalance(final String iban) {
        return doInJDBC(connection -> {
            return getBalance(connection, iban);
        });
    }

    private void addBalance(Connection connection, final String iban, long balance) {
        try(PreparedStatement statement = connection.prepareStatement(
            "UPDATE account " +
            "SET balance = balance + ? " +
            "WHERE iban = ?")
        ) {
            statement.setLong(1, balance);
            statement.setString(2, iban);

            statement.executeUpdate();
        } catch (SQLException e) {
            throw new IllegalStateException(e);
        }
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

        public long getBalance() {
            return balance;
        }

        public void setBalance(long balance) {
            this.balance = balance;
        }
    }
}
