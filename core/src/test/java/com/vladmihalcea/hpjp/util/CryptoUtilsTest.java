package com.vladmihalcea.hpjp.util;

import com.codahale.metrics.MetricRegistry;
import com.codahale.metrics.Slf4jReporter;
import com.codahale.metrics.Timer;
import com.vladmihalcea.hpjp.spring.transaction.readonly.config.stats.SpringTransactionStatisticsReport;
import org.hibernate.internal.util.collections.BoundedConcurrentHashMap;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;
import java.util.stream.LongStream;

import static org.junit.Assert.assertEquals;

/**
 * @author  Vlad Mihalcea
 */
public final class CryptoUtilsTest {

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
    private int MAX_COUNT = 100_000;

    private final BoundedConcurrentHashMap<Long, String> encryptCache = new BoundedConcurrentHashMap<>(
        100_000,
        20,
        BoundedConcurrentHashMap.Eviction.LIRS
    );

    private final BoundedConcurrentHashMap<String, Long> decryptCache = new BoundedConcurrentHashMap<>(
        100_000,
        20,
        BoundedConcurrentHashMap.Eviction.LIRS
    );

    private int encryptCallCount = 0;
    private int decryptCallCount = 0;

    @Test
    public void testPerformance() {
        if(!AbstractTest.ENABLE_LONG_RUNNING_TESTS) {
            return;
        }
        warmUp();

        LongStream.rangeClosed(1, MAX_COUNT).forEach(i -> {
            Long value = random.nextLong(i);
            long startNanos = System.nanoTime();
            String encryptedValue = CryptoUtils.encrypt(value);
            encryptTimer.update((System.nanoTime() - startNanos), TimeUnit.NANOSECONDS);

            startNanos = System.nanoTime();
            Long decryptedValue = CryptoUtils.decrypt(encryptedValue, Long.class);
            decryptTimer.update((System.nanoTime() - startNanos), TimeUnit.NANOSECONDS);
            assertEquals(value.longValue(), decryptedValue.longValue());
        });

        logReporter.report();
    }

    @Test
    public void testPerformanceUsingCache() {
        if(!AbstractTest.ENABLE_LONG_RUNNING_TESTS) {
            return;
        }
        warmUp();

        LongStream.rangeClosed(1, MAX_COUNT).forEach(i -> {
            //Hit ratio of 90.7%
            long threshold = i > 10 ? i / 10 : i;
            Long value = random.nextLong(threshold);
            long startNanos = System.nanoTime();
            String encryptedValue = encryptCache.get(value);
            if(encryptedValue == null) {
                encryptCallCount++;
                encryptedValue = CryptoUtils.encrypt(value);
                encryptCache.put(value, encryptedValue);
            }
            encryptTimer.update((System.nanoTime() - startNanos), TimeUnit.NANOSECONDS);

            startNanos = System.nanoTime();
            Long decryptedValue = decryptCache.get(encryptedValue);
            if(decryptedValue == null) {
                decryptCallCount++;
                decryptedValue = CryptoUtils.decrypt(encryptedValue, Long.class);
                decryptCache.put(encryptedValue, decryptedValue);
            }
            decryptTimer.update((System.nanoTime() - startNanos), TimeUnit.NANOSECONDS);
            assertEquals(value.longValue(), decryptedValue.longValue());
        });

        logReporter.report();
        LOGGER.info("Encrypt was called {} times", encryptCallCount);
        LOGGER.info("Decrypt was called {} times", decryptCallCount);
    }

    private void warmUp() {
        LongStream.rangeClosed(1, MAX_COUNT/10).forEach(i -> {
            Long value = random.nextLong();
            String encryptedValue = CryptoUtils.encrypt(value);
            Long decryptedValue = CryptoUtils.decrypt(encryptedValue, Long.class);
            assertEquals(value.longValue(), decryptedValue.longValue());
        });
    }
}
