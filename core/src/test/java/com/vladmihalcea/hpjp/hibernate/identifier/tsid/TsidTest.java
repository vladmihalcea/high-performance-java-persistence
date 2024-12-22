package com.vladmihalcea.hpjp.hibernate.identifier.tsid;

import com.vladmihalcea.hpjp.util.TsidUtils;
import io.hypersistence.tsid.TSID;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.security.SecureRandom;
import java.text.DecimalFormat;
import java.util.Random;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.IntSupplier;

import static org.junit.Assert.assertNull;

public class TsidTest {

    protected final Logger LOGGER = LoggerFactory.getLogger(getClass());

    @Test
    public void test() {
        TSID tsid = TSID.fast();
        long tsidLong = tsid.toLong();
        String tsidString = tsid.toString();
        long tsidMillis = tsid.getUnixMilliseconds();

        LOGGER.info("TSID numerical value: {}", tsidLong);
        LOGGER.info("TSID string value: {}", tsidString);
        LOGGER.info("TSID time millis since epoch value: {}", tsidMillis);

        for (int i = 0; i < 10; i++) {
            LOGGER.info(
                "TSID numerical value: {}",
                TSID.fast().toLong()
            );
        }
    }

    @Test
    public void testNodeCount() {
        TSID.Factory tsidFactory = TsidUtils.getTsidFactory(27, 11);
        tsidFactory.generate();
    }

    @Test
    public void testConcurrency() throws InterruptedException {
        int threadCount = 16;
        int iterationCount = 100_000;

        CountDownLatch endLatch = new CountDownLatch(threadCount);

        ConcurrentMap<TSID, Integer> tsidMap = new ConcurrentHashMap<>();

        long startNanos = System.nanoTime();

        AtomicLong collisionCount = new AtomicLong();

        int nodeCount = 2;

        for (int i = 0; i < threadCount; i++) {
            final int threadId = i;
            new Thread(() -> {
                TSID.Factory tsidFactory = TsidUtils.getTsidFactory(nodeCount, threadId % nodeCount);

                for (int j = 0; j < iterationCount; j++) {
                    TSID tsid = tsidFactory.generate();
                    Integer existingTsid = tsidMap.put(tsid, (threadId * iterationCount) + j);
                    if(existingTsid != null) {
                        collisionCount.incrementAndGet();
                    }
                }

                endLatch.countDown();
            }).start();
        }
        LOGGER.info("Starting threads");
        endLatch.await();

        LOGGER.info(
            "{} threads generated {} TSIDs in {} ms with {} collisions",
            threadCount,
            new DecimalFormat("###,###,###").format(
                threadCount * iterationCount
            ),
            TimeUnit.NANOSECONDS.toMillis(
                System.nanoTime() - startNanos
            ),
            collisionCount
        );
    }

    @Test
    public void testConcurrencyNoConflict() throws InterruptedException {
        int threadCount = 16;
        int iterationCount = 100_000;

        CountDownLatch endLatch = new CountDownLatch(threadCount);

        ConcurrentMap<TSID, Integer> tsidMap = new ConcurrentHashMap<>();

        long startNanos = System.nanoTime();

        AtomicLong collisionCount = new AtomicLong();

        int nodeCount = 2;

        for (int i = 0; i < threadCount; i++) {
            final int threadId = i;
            new Thread(() -> {
                int nodeId = threadId % nodeCount;
                int nodeBits = (int) (Math.log(nodeCount) / Math.log(2));

                final Random random = new SecureRandom();

                TSID.Factory.Builder builder = TSID.Factory.builder();
                builder.withNodeBits(nodeBits);
                builder.withNode(nodeId);
                builder.withRandomFunction(new IntSupplier() {
                    @Override
                    public synchronized int getAsInt() {
                        return random.nextInt();
                    }
                });
                TSID.Factory tsidFactory = builder
                    .build();

                for (int j = 0; j < iterationCount; j++) {
                    TSID tsid = tsidFactory.generate();
                    Integer existingTsid = tsidMap.put(tsid, (threadId * iterationCount) + j);
                    if(existingTsid != null) {
                        collisionCount.incrementAndGet();
                    }
                }

                endLatch.countDown();
            }).start();
        }
        LOGGER.info("Starting threads");
        endLatch.await();

        LOGGER.info(
            "{} threads generated {} TSIDs in {} ms with {} collisions",
            threadCount,
            new DecimalFormat("###,###,###").format(
                threadCount * iterationCount
            ),
            TimeUnit.NANOSECONDS.toMillis(
                System.nanoTime() - startNanos
            ),
            collisionCount
        );
    }

}
