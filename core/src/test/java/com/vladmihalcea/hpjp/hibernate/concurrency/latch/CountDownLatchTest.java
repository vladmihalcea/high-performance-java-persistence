package com.vladmihalcea.hpjp.hibernate.concurrency.latch;

import com.vladmihalcea.hpjp.hibernate.concurrency.acid.Account;
import com.vladmihalcea.hpjp.util.AbstractTest;
import com.vladmihalcea.hpjp.util.providers.Database;
import com.vladmihalcea.hpjp.util.transaction.ConnectionCallable;
import com.vladmihalcea.hpjp.util.transaction.ConnectionVoidCallable;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import static org.junit.Assert.assertEquals;

/**
 * @author Vlad Mihalcea
 */
public class CountDownLatchTest {

    protected final Logger LOGGER = LoggerFactory.getLogger(getClass());

    protected final ExecutorService executorService = Executors.newSingleThreadExecutor();

    @Test
    public void testNoCoordination() {
        LOGGER.info("Main thread starts");

        int workerThreadCount = 5;

        for (int i = 1; i <= workerThreadCount; i++) {
            String threadId = String.valueOf(i);
            new Thread(
                () -> LOGGER.info("Worker thread {} runs", threadId),
                "Thread-" + threadId
            ).start();
        }

        LOGGER.info("Main thread finishes");
    }

    @Test
    public void testNoCoordinationExecutorService() {
        LOGGER.info("Main thread starts");

        executorService.submit(() -> {
            LOGGER.info("Worker thread runs");
        });

        LOGGER.info("Main thread finishes");
    }

    @Test
    public void testCountDownLatch() throws InterruptedException {
        LOGGER.info("Main thread starts");

        int workerThreadCount = 5;

        CountDownLatch endLatch = new CountDownLatch(workerThreadCount);

        for (int i = 1; i <= workerThreadCount; i++) {
            String threadId = String.valueOf(i);
            new Thread(
                () -> {
                    LOGGER.info("Worker thread {} runs", threadId);

                    endLatch.countDown();
                },
                "Thread-" + threadId
            ).start();
        }

        LOGGER.info("Main thread waits for the worker threads to finish");
        endLatch.await();

        LOGGER.info("Main thread finishes");
    }
}
