package com.vladmihalcea.book.hpjp.hibernate.concurrency.acid;

import com.vladmihalcea.book.hpjp.util.AbstractTest;
import org.junit.Test;

import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Table;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.*;
import java.util.stream.IntStream;

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
        long fromBalance = from.getBalance();

        if(fromBalance >= transferCents) {
            from.addBalance(-1 * transferCents);
            to.addBalance(transferCents);
        }
    }

    @Test
    public void testParallelExecution() {

        Account fromAccount = new Account();
        fromAccount.setIban("Alice-123");
        fromAccount.setOwner("Alice");
        fromAccount.setBalance(10);

        Account toAccount = new Account();
        toAccount.setIban("Bob-456");
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

        LOGGER.info("Alice's balance {}", fromAccount.getBalance());
        LOGGER.info("Bob's balance {}", toAccount.getBalance());
    }

    public static class Account {

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

        public void addBalance(long amount) {
            this.balance += amount;
        }
    }
}
