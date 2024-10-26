package com.vladmihalcea.hpjp.spring.transaction.transfer;

import com.vladmihalcea.flexypool.FlexyPoolDataSource;
import com.vladmihalcea.hpjp.spring.common.AbstractSpringTest;
import com.vladmihalcea.hpjp.spring.transaction.transfer.config.FlexyPoolACIDRaceConditionTransferConfiguration;
import com.vladmihalcea.hpjp.spring.transaction.transfer.domain.Account;
import com.vladmihalcea.hpjp.spring.transaction.transfer.repository.AccountRepository;
import com.vladmihalcea.hpjp.spring.transaction.transfer.service.TransferService;
import com.zaxxer.hikari.HikariDataSource;
import net.ttddyy.dsproxy.support.ProxyDataSource;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.transaction.TransactionException;
import org.springframework.transaction.support.TransactionCallback;

import javax.sql.DataSource;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import static org.junit.Assert.assertEquals;

/**
 * @author Vlad Mihalcea
 */
@ContextConfiguration(classes = FlexyPoolACIDRaceConditionTransferConfiguration.class)
public class FlexyPoolACIDRaceConditionTransferTest extends AbstractSpringTest {

    @Autowired
    private TransferService transferService;

    @Autowired
    private AccountRepository accountRepository;

    @Autowired
    private ProxyDataSource dataSource;

    @Override
    protected Class<?>[] entities() {
        return new Class[]{
            Account.class
        };
    }

    @Override
    public void afterInit() {
        try {
            transactionTemplate.execute((TransactionCallback<Void>) transactionStatus -> {
                entityManager.persist(
                    new Account()
                        .setId("Alice-123")
                        .setOwner("Alice")
                        .setBalance(10)
                );

                entityManager.persist(
                    new Account()
                        .setId("Bob-456")
                        .setOwner("Bob")
                        .setBalance(0)
                );
                
                return null;
            });
        } catch (TransactionException e) {
            LOGGER.error("Failure", e);
        }

    }

    @Test
    public void testParallelExecution() throws InterruptedException {
        assertEquals(10L, accountRepository.getBalance("Alice-123"));
        assertEquals(0L, accountRepository.getBalance("Bob-456"));

        long startNanos = System.nanoTime();
        int threadCount = 64;

        CountDownLatch startLatch = new CountDownLatch(1);
        CountDownLatch endLatch = new CountDownLatch(threadCount);

        for (int i = 0; i < threadCount; i++) {
            new Thread(() -> {
                try {
                    startLatch.await();

                    transferService.transfer("Alice-123", "Bob-456", 5L);
                } catch (Exception e) {
                    LOGGER.error("Transfer failed", e);
                } finally {
                    endLatch.countDown();
                }
            }).start();
        }
        LOGGER.info("Starting threads");
        startLatch.countDown();
        endLatch.await();

        FlexyPoolDataSource<HikariDataSource> flexyPoolDataSource = (FlexyPoolDataSource<HikariDataSource>) dataSource.getDataSource();
        HikariDataSource hikariDataSource = flexyPoolDataSource.getTargetDataSource();

        LOGGER.info(
            "The {} transfers were executed on {} database connections in {} ms",
            threadCount,
            hikariDataSource.getMaximumPoolSize(),
            TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - startNanos)
        );

        LOGGER.info("Alice's balance: {}", accountRepository.getBalance("Alice-123"));
        LOGGER.info("Bob's balance: {}", accountRepository.getBalance("Bob-456"));
    }
}
