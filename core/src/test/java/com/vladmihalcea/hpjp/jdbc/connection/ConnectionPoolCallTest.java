package com.vladmihalcea.hpjp.jdbc.connection;

import com.codahale.metrics.MetricRegistry;
import com.codahale.metrics.Slf4jReporter;
import com.codahale.metrics.Timer;
import com.vladmihalcea.hpjp.util.DatabaseProviderIntegrationTest;
import com.vladmihalcea.hpjp.util.providers.Database;
import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import org.junit.Ignore;
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
@Ignore
public class ConnectionPoolCallTest extends DatabaseProviderIntegrationTest {

    private final Logger LOGGER = LoggerFactory.getLogger(getClass());

    private MetricRegistry metricRegistry = new MetricRegistry();

    private Timer timer = metricRegistry.timer("connectionTimer");

    private Slf4jReporter logReporter = Slf4jReporter
            .forRegistry(metricRegistry)
            .outputTo(LOGGER)
            .build();

    private int warmingUpCount = 100;
    private int connectionAcquisitionCount = 1000;

    public ConnectionPoolCallTest(Database database) {
        super(database);
    }

    @Override
    protected Class<?>[] entities() {
        return new Class[]{};
    }

    @Test
    public void testNoPooling() throws SQLException {
        LOGGER.info("Test without pooling for {}", dataSourceProvider().database());
        test(dataSourceProvider().dataSource());
    }

    @Test
    public void testPooling() throws SQLException {
        LOGGER.info("Test with pooling for {}", dataSourceProvider().database());
        test(poolingDataSource());
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

    protected HikariDataSource poolingDataSource() {
        HikariConfig config = new HikariConfig();
        config.setJdbcUrl(dataSourceProvider().url());
        config.setUsername(dataSourceProvider().username());
        config.setPassword(dataSourceProvider().password());
        return new HikariDataSource(config);
    }
}
