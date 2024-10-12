package com.vladmihalcea.hpjp.util.resources;

import com.codahale.metrics.MetricRegistry;
import com.codahale.metrics.Slf4jReporter;
import com.codahale.metrics.Timer;
import com.vladmihalcea.hpjp.spring.transaction.readonly.config.stats.SpringTransactionStatisticsReport;
import com.vladmihalcea.hpjp.util.AbstractTest;
import com.vladmihalcea.hpjp.util.CryptoUtils;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.*;

/**
 * @author  Vlad Mihalcea
 */
public final class CpuTest {

    public static Logger LOGGER = LoggerFactory.getLogger(SpringTransactionStatisticsReport.class);

    private MetricRegistry metricRegistry = new MetricRegistry();

    private Slf4jReporter logReporter = Slf4jReporter
        .forRegistry(metricRegistry)
        .outputTo(LOGGER)
        .convertDurationsTo(TimeUnit.MICROSECONDS)
        .build();

    private final Timer encryptTimer = metricRegistry.timer("encryptTimer");
    private final Timer decryptTimer = metricRegistry.timer("decryptTimer");

    private final ThreadLocalRandom random = ThreadLocalRandom.current();

    private final long MAX_EXECUTION_TIME_NANOS = TimeUnit.MINUTES.toNanos(5);

    @Test
    public void testOneCore() {
        if(!AbstractTest.ENABLE_LONG_RUNNING_TESTS) {
            return;
        }
        process();
    }

    @Test
    public void testMultipleCores() {
        if(!AbstractTest.ENABLE_LONG_RUNNING_TESTS) {
            return;
        }

        int threadCount = 7;
        ExecutorService executorService = Executors.newFixedThreadPool(threadCount);
        List<Future> futures = new ArrayList<>(threadCount);
        for (int i = 0; i < threadCount; i++) {
            futures.add(executorService.submit(this::process));
        }
        for (int i = 0; i < threadCount; i++) {
            try {
                futures.get(i).get();
            } catch (InterruptedException e) {
                Thread.interrupted();
            } catch (ExecutionException e) {
                throw new RuntimeException(e);
            }
        }
        try {
            executorService.awaitTermination(MAX_EXECUTION_TIME_NANOS, TimeUnit.NANOSECONDS);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
    }

    private void process() {
        long startNanos = System.nanoTime();
        int messageLength = 100;
        long previousElapsedSecond = 0;

        while ((System.nanoTime() - startNanos) < MAX_EXECUTION_TIME_NANOS) {
            BigInteger.valueOf(random.nextLong())
                .multiply(BigInteger.valueOf(random.nextLong()));

            StringBuilder stringBuilder = new StringBuilder();
            byte[] bytes = new byte[16];
            for (int i = 0; i < messageLength; i++) {
                random.nextBytes(bytes);
                stringBuilder.append(new String(bytes));
            }
            String message = stringBuilder.toString();

            long startEncryptNanos = System.nanoTime();
            String encryptedValue = CryptoUtils.encrypt(message);
            encryptTimer.update((System.nanoTime() - startEncryptNanos), TimeUnit.NANOSECONDS);

            long startDecryptNanos = System.nanoTime();
            String decryptedValue = CryptoUtils.decrypt(encryptedValue, String.class);
            decryptTimer.update((System.nanoTime() - startDecryptNanos), TimeUnit.NANOSECONDS);

            long elapsedSeconds = TimeUnit.NANOSECONDS.toSeconds(System.nanoTime() - startNanos);

            if(elapsedSeconds > 0 && previousElapsedSecond != elapsedSeconds && elapsedSeconds % 5 == 0) {
                LOGGER.info("Elapsed {} seconds", elapsedSeconds);
                previousElapsedSecond = elapsedSeconds;
                logReporter.report();
            }
        }
    }

    protected void sleep(long millis) {
        try {
            Thread.sleep(millis);
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }
}
