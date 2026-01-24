package com.vladmihalcea.hpjp.jdbc.fetching.reservoir;

import com.codahale.metrics.*;
import org.junit.Ignore;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;

/**
 * @author Vlad Mihalcea
 */
public class SlidingTimeWindowMetricsTest {

    protected final Logger LOGGER = LoggerFactory.getLogger(getClass());

    private long primeNanos = System.nanoTime();

    @Test
    @Ignore
    public void testFullSample() {
        int iterations = 100_000_000;
        Reservoir reservoir = execute(iterations);
        printMetrics(reservoir);
    }

    @Test
    @Ignore
    public void testRecentChanges() {
        Reservoir reservoir = new SlidingTimeWindowReservoir(5, TimeUnit.SECONDS);
        for(int i = 1; i <= 1028; i++) {
            reservoir.update(1);
        }
        printMetrics(reservoir);

        sleep(TimeUnit.SECONDS.toMillis(5));

        for(int i = 1; i <= 100; i++) {
            reservoir.update(10);
        }
        printMetrics(reservoir);
    }

    private void sleep(long millis) {
        try {
            Thread.sleep(millis);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
    }

    public Reservoir execute(int iterations) {
        Reservoir reservoir = new SlidingTimeWindowReservoir(5, TimeUnit.MINUTES);
        String sql = "SELECT COUNT(*) FROM Users";

        for (int i = 0; i < iterations; i++) {
            long startNanos = System.nanoTime();
            executeQuery(sql);
            long endNanos = System.nanoTime();
            reservoir.update(
                TimeUnit.NANOSECONDS.toMicros(endNanos - startNanos)
            );
            if(i > 0 && i % 1_000_000 == 0) {
                LOGGER.info("Collection size={}", i);
            }
        }
        return reservoir;
    }

    private void printMetrics(Reservoir reservoir) {
        Snapshot snapshot = reservoir.getSnapshot();
        LOGGER.info("""
            Collection size={}, min={}, max={}, mean={}, stddev={},
            median={}, p75={}, p95={}, p98={}, time unit=μs
            """,
            snapshot.size(), snapshot.getMin(), snapshot.getMax(),
            snapshot.getMean(), snapshot.getStdDev(),
            snapshot.getValue(0.5), snapshot.getValue(0.75),
            snapshot.getValue(0.95), snapshot.getValue(0.98)
        );
    }

    private void executeQuery(String sql) {
        long sleepNanos = System.nanoTime() - primeNanos;
            ThreadLocalRandom.current().nextInt(100);
        try {
            Thread.sleep(TimeUnit.NANOSECONDS.toMillis(sleepNanos));
        } catch (InterruptedException e) {
            Thread.interrupted();
        }
    }
}
