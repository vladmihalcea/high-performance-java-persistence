package com.vladmihalcea.hpjp.jdbc.fetching;

import com.codahale.metrics.UniformSnapshot;
import com.vladmihalcea.hpjp.hibernate.forum.Post;
import com.vladmihalcea.hpjp.hibernate.forum.PostComment;
import com.vladmihalcea.hpjp.hibernate.forum.PostDetails;
import com.vladmihalcea.hpjp.hibernate.forum.Tag;
import com.vladmihalcea.hpjp.util.AbstractTest;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.Duration;
import java.time.temporal.ChronoUnit;
import java.time.temporal.TemporalUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.*;
import java.util.stream.Collectors;
import java.util.stream.LongStream;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

/**
 * @author Vlad Mihalcea
 */
public class MetricsTest {

    protected final Logger LOGGER = LoggerFactory.getLogger(getClass());

    @Test
    public void testFullSample() {
        int iterations = 100_000_000;
        execute(iterations);
    }

    @Test
    public void testSmallSample() {
        List<Long> queryExecutionTimes = execute(10);
        LOGGER.info("Response times: {}", queryExecutionTimes);
        printMetrics(queryExecutionTimes);
    }

    public List<Long> execute(int iterations) {
        List<Long> queryExecutionTimes = new ArrayList<>();
        String sql = "SELECT COUNT(*) FROM Users";

        for (int i = 0; i < iterations; i++) {
            long startNanos = System.nanoTime();
            executeQuery(sql);
            long endNanos = System.nanoTime();
            queryExecutionTimes.add(
                TimeUnit.NANOSECONDS.toMicros(endNanos - startNanos)
            );
            if(i > 0 && i % 1_000_000 == 0) {
                LOGGER.info("Collection size={}", i);
            }
        }
        return queryExecutionTimes;
    }

    private void executeQuery(String sql) {
        long sleepNanos = ThreadLocalRandom.current().nextInt(100);
        try {
            Thread.sleep(TimeUnit.NANOSECONDS.toMillis(sleepNanos));
        } catch (InterruptedException e) {
            Thread.interrupted();
        }
    }

    private void printMetrics(List<Long> values) {
        UniformSnapshot snapshot = new UniformSnapshot(values);
        LOGGER.info("""
            Collection size={}, min={}, max={}, mean={}, stddev={}
            median={}, p75={}, p95={}, p98={}, time unit=μs
            """,
            snapshot.size(), snapshot.getMin(), snapshot.getMax(),
            snapshot.getMean(), snapshot.getStdDev(),
            snapshot.getValue(0.5), snapshot.getValue(0.75),
            snapshot.getValue(0.95), snapshot.getValue(0.98)
        );
    }
}
