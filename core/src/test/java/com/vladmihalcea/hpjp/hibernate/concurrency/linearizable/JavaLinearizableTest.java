package com.vladmihalcea.hpjp.hibernate.concurrency.linearizable;

import com.vladmihalcea.hpjp.util.AbstractTest;
import org.junit.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.*;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

/**
 * @author Vlad Mihalcea
 */
public class JavaLinearizableTest extends AbstractTest {

    @Override
    protected Class<?>[] entities() {
        return new Class<?>[]{
            Account.class
        };
    }

    int threadCount = 32;

    public static ForkJoinPool forkJoinPool = ForkJoinPool.commonPool();

    public void transfer(Account from, Account to, long transferCents) {
        long fromBalance = from.getAccountBalance();

        if(fromBalance >= transferCents) {
            from.addToAccountBalance(-1 * transferCents);
            to.addToAccountBalance(transferCents);
        }
    }

    @Test
    public void testParallelExecution() {

        Account fromAccount = new Account();
        fromAccount.setId("Alice-123");
        fromAccount.setOwner("Alice");
        fromAccount.setBalance(10);

        Account toAccount = new Account();
        toAccount.setId("Bob-456");
        toAccount.setOwner("Bob");
        toAccount.setBalance(0L);

        List<Callable<Void>> callables = new ArrayList<>();

        for (int i = 0; i < threadCount; i++) {
            callables.add(() -> {
                transfer(fromAccount, toAccount, 5);

                return null;
            });
        }

        LOGGER.info("Starting threads");
        List<Future<Void>> futures = forkJoinPool.invokeAll(callables);
        for (Future future : futures) {
            try {
                future.get();
            } catch (InterruptedException|ExecutionException e) {
                fail(e.getMessage());
            }
        }

        LOGGER.info("Alice's balance {}", fromAccount.getAccountBalance());
        LOGGER.info("Bob's balance {}", toAccount.getAccountBalance());
    }

    public static class Account {

        private String id;

        private String owner;

        private long balance;

        public String getId() {
            return id;
        }

        public void setId(String id) {
            this.id = id;
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

        public void addToAccountBalance(long amount) {
            this.balance += amount;
        }
    }
}
