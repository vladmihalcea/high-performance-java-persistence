package com.vladmihalcea.hpjp.spring.transaction.transfer;

import com.vladmihalcea.hpjp.spring.common.AbstractSpringTest;
import com.vladmihalcea.hpjp.spring.transaction.transfer.config.ACIDRaceConditionTransferConfiguration;
import com.vladmihalcea.hpjp.spring.transaction.transfer.domain.Account;
import com.vladmihalcea.hpjp.spring.transaction.transfer.repository.AccountRepository;
import com.vladmihalcea.hpjp.spring.transaction.transfer.service.TransferService;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.transaction.TransactionException;
import org.springframework.transaction.support.TransactionCallback;

import java.util.Collection;
import java.util.List;
import java.util.concurrent.*;
import java.util.function.IntFunction;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static org.junit.Assert.assertEquals;

/**
 * @author Vlad Mihalcea
 */
@ContextConfiguration(classes = ACIDRaceConditionTransferConfiguration.class)
public class ACIDRaceConditionTransferTest extends AbstractSpringTest {

    @Autowired
    private TransferService transferService;

    @Autowired
    private AccountRepository accountRepository;

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
    public void testSerialExecution() {
        assertEquals(10L, accountRepository.getBalance("Alice-123"));
        assertEquals(0L, accountRepository.getBalance("Bob-456"));

        transferService.transfer("Alice-123", "Bob-456", 5L);

        assertEquals(5L, accountRepository.getBalance("Alice-123"));
        assertEquals(5L, accountRepository.getBalance("Bob-456"));

        transferService.transfer("Alice-123", "Bob-456", 5L);

        assertEquals(0L, accountRepository.getBalance("Alice-123"));
        assertEquals(10L, accountRepository.getBalance("Bob-456"));

        transferService.transfer("Alice-123", "Bob-456", 5L);

        assertEquals(0L, accountRepository.getBalance("Alice-123"));
        assertEquals(10L, accountRepository.getBalance("Bob-456"));
    }

    //Maximum connection count is limited to 64 due to Hikari maximum pool size
    private int threadCount = 16;

    @Test
    public void testParallelExecution() throws InterruptedException {
        assertEquals(10L, accountRepository.getBalance("Alice-123"));
        assertEquals(0L, accountRepository.getBalance("Bob-456"));

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

        LOGGER.info("Alice's balance: {}", accountRepository.getBalance("Alice-123"));
        LOGGER.info("Bob's balance: {}", accountRepository.getBalance("Bob-456"));
    }

    @Test
    public void testParallelExecutionUsingExecutorService() throws InterruptedException {

        ExecutorService executorService = Executors.newFixedThreadPool(threadCount);

        assertEquals(10L, accountRepository.getBalance("Alice-123"));
        assertEquals(0L, accountRepository.getBalance("Bob-456"));

        LOGGER.info("Starting threads");

        Collection<Callable<Void>> callables = IntStream
            .range(0, threadCount)
            .mapToObj( i -> (Callable<Void>) () -> {
                    transferService.transfer("Alice-123", "Bob-456", 5L);
                    return null;
                }
            )
            .toList();

        List<Future<Void>> futures = executorService.invokeAll(callables);
        for (Future<Void> future : futures) {
            try {
                future.get();
            } catch (InterruptedException| ExecutionException e) {
                LOGGER.error(e.getMessage());
            }
        }

        LOGGER.info("Alice's balance {}", accountRepository.getBalance("Alice-123"));
        LOGGER.info("Bob's balance {}", accountRepository.getBalance("Bob-456"));
    }
}
