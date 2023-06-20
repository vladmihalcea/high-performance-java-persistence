package com.vladmihalcea.hpjp.spring.transaction.readonly.config.stats;

import com.codahale.metrics.MetricRegistry;
import com.codahale.metrics.Slf4jReporter;
import com.codahale.metrics.Timer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.TimeUnit;

/**
 * @author Vlad Mihalcea
 */
public class SpringTransactionStatisticsReport {

    public static Logger LOGGER = LoggerFactory.getLogger(SpringTransactionStatisticsReport.class);

    private MetricRegistry metricRegistry = new MetricRegistry();

    private Slf4jReporter logReporter = Slf4jReporter
        .forRegistry(metricRegistry)
        .outputTo(LOGGER)
        .build();

    private Timer transactionTimer = metricRegistry.
        timer("transactionTimer");

    private Timer fxRateTimer = metricRegistry.timer("fxRateTimer");

    public void transactionTime(long nanos) {
        transactionTimer.update(nanos, TimeUnit.NANOSECONDS);
    }

    public void fxRateTime(long nanos) {
        fxRateTimer.update(nanos, TimeUnit.NANOSECONDS);
    }

    public void generate() {
        logReporter.report();
    }
}
