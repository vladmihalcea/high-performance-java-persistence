package com.vladmihalcea.book.high_performance_java_persistence.jdbc.connection;

import com.codahale.metrics.MetricRegistry;
import com.codahale.metrics.Slf4jReporter;
import com.codahale.metrics.Timer;
import com.vladmihalcea.book.high_performance_java_persistence.util.AbstractOracleXEIntegrationTest;
import com.vladmihalcea.book.high_performance_java_persistence.util.AbstractTest;
import com.vladmihalcea.book.high_performance_java_persistence.util.DataSourceProviderIntegrationTest;
import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.Properties;
import java.util.concurrent.TimeUnit;

/**
 * <code>ConnectionPoolCallTest</code> - Checks how connection pool decreases latency
 *
 * @author Vlad Mihalcea
 */
public class OracleConnectionCallTest extends AbstractOracleXEIntegrationTest {

    private final Logger LOGGER = LoggerFactory.getLogger(getClass());

    private int callCount = 1000;

    @Override
    protected Class<?>[] entities() {
        return new Class[]{};
    }

    @Test
    public void testConnections() throws SQLException {
        LOGGER.info("Test without pooling for {}", getDataSourceProvider().database());
        simulateLowLatencyTransactions(getDataSourceProvider().dataSource(), 10);
        simulateLowLatencyTransactions(getDataSourceProvider().dataSource(), 35);
    }

    private void simulateLowLatencyTransactions(
            DataSource dataSource, int waitMillis)
            throws SQLException {
        for (int i = 0; i < callCount; i++) {
            try {
                try (Connection connection =
                     dataSource.getConnection()) {
                    sleep(waitMillis);
                }
            } catch (SQLException e) {
                LOGGER.error("Exception on iteration " + i, e);
            }
        }
    }
}
