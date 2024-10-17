package com.vladmihalcea.hpjp.jdbc.connection;

import com.codahale.metrics.MetricRegistry;
import com.codahale.metrics.Slf4jReporter;
import com.codahale.metrics.Timer;
import com.vladmihalcea.flexypool.FlexyPoolDataSource;
import com.vladmihalcea.flexypool.adaptor.HikariCPPoolAdapter;
import com.vladmihalcea.flexypool.config.FlexyPoolConfiguration;
import com.vladmihalcea.flexypool.strategy.IncrementPoolOnTimeoutConnectionAcquisitionStrategy;
import com.vladmihalcea.hpjp.util.AbstractTest;
import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.concurrent.TimeUnit;

/**
 * @author Vlad Mihalcea
 */
public class FlexyPoolAutoSizingTest extends AbstractTest {

    private final Logger LOGGER = LoggerFactory.getLogger(getClass());

    private MetricRegistry metricRegistry = new MetricRegistry();

    private Timer timer = metricRegistry.timer("connectionTimer");

    private Slf4jReporter logReporter = Slf4jReporter
            .forRegistry(metricRegistry)
            .outputTo(LOGGER)
            .build();

    private int warmingUpCount = 100;
    private int connectionAcquisitionCount = 1000;

    @Override
    protected Class<?>[] entities() {
        return new Class[]{};
    }

    @Test
    public void testPooling() throws SQLException {
        LOGGER.info("Test with pooling for {}", dataSourceProvider().database());
        DataSource poolingDataSource = poolingDataSource();
    }

    private void test(DataSource dataSource) throws SQLException {
        //Warming up
        for (int i = 0; i < warmingUpCount; i++) {
            try (Connection connection = dataSource.getConnection()) {
            }
        }
        for (int i = 0; i < connectionAcquisitionCount; i++) {
            long startNanos = System.nanoTime();
            try (Connection connection = dataSource.getConnection()) {
            }
            timer.update(System.nanoTime() - startNanos, TimeUnit.NANOSECONDS);
        }
        logReporter.report();
    }

    protected DataSource poolingDataSource() {
        HikariConfig hikariConfig = new HikariConfig();
        hikariConfig.setJdbcUrl(dataSourceProvider().url());
        hikariConfig.setUsername(dataSourceProvider().username());
        hikariConfig.setPassword(dataSourceProvider().password());
        hikariConfig.setMaximumPoolSize(1);
        HikariDataSource connectionPoolDataSource = new HikariDataSource(hikariConfig);

        FlexyPoolConfiguration<HikariDataSource> flexyPoolConfiguration = new FlexyPoolConfiguration
            .Builder<>(
                getClass().getSimpleName(),
                connectionPoolDataSource,
                HikariCPPoolAdapter.FACTORY
            )
            .setMetricLogReporterMillis(TimeUnit.SECONDS.toMillis(15))
            .build();

        FlexyPoolDataSource<HikariDataSource> flexyPoolDataSource = new FlexyPoolDataSource<>(
            flexyPoolConfiguration,
            new IncrementPoolOnTimeoutConnectionAcquisitionStrategy.Factory<>(10, 200)
        );

        return flexyPoolDataSource;
    }
}
