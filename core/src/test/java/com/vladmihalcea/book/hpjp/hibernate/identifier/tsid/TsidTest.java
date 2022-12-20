package com.vladmihalcea.book.hpjp.hibernate.identifier.tsid;

import com.github.f4b6a3.tsid.Tsid;
import com.github.f4b6a3.tsid.TsidCreator;
import com.vladmihalcea.book.hpjp.util.TsidUtils;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.text.DecimalFormat;
import java.util.concurrent.*;

import static org.junit.Assert.assertNull;


public class TsidTest {

    protected final Logger LOGGER = LoggerFactory.getLogger(getClass());

    @Test
    public void test() {
        Tsid tsid = TsidCreator.getTsid256();
        long tsidLong = tsid.toLong();
        String tsidString = tsid.toString();
        long tsidMillis = tsid.getUnixMilliseconds();

        LOGGER.info("TSID numerical value: {}", tsidLong);
        LOGGER.info("TSID string value: {}", tsidString);
        LOGGER.info("TSID time millis since epoch value: {}", tsidMillis);

        for (int i = 0; i < 10; i++) {
            LOGGER.info(
                "TSID numerical value: {}",
                TsidCreator.getTsid256().toLong()
            );
        }
    }

    @Test
    public void testConcurrency() throws InterruptedException {
        int threadCount = 16;
        int iterationCount = 100_000;

        CountDownLatch endLatch = new CountDownLatch(threadCount);

        ConcurrentMap<Tsid, Integer> tsidMap = new ConcurrentHashMap<>();

        long startNanos = System.nanoTime();

        for (int i = 0; i < threadCount; i++) {
            final int threadId = i;
            new Thread(() -> {
                for (int j = 0; j < iterationCount; j++) {
                    Tsid tsid = TsidUtils.TSID_FACTORY.create();
                    assertNull(
                        "TSID collision detected",
                        tsidMap.put(tsid, (threadId * iterationCount) + j)
                    );
                }

                endLatch.countDown();
            }).start();
        }
        LOGGER.info("Starting threads");
        endLatch.await();

        LOGGER.info(
            "{} threads generated {} TSIDs in {} ms",
            threadCount,
            new DecimalFormat("###,###,###").format(
                threadCount * iterationCount
            ),
            TimeUnit.NANOSECONDS.toMillis(
                System.nanoTime() - startNanos
            )
        );
    }

}
