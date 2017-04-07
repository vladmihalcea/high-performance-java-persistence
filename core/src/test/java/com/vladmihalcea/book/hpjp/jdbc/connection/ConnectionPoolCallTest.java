package com.vladmihalcea.book.hpjp.jdbc.connection;

import com.codahale.metrics.MetricRegistry;
import com.codahale.metrics.Slf4jReporter;
import com.codahale.metrics.Timer;
import com.vladmihalcea.book.hpjp.util.DataSourceProviderIntegrationTest;
import com.vladmihalcea.book.hpjp.util.providers.DataSourceProvider;
import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import org.junit.Ignore;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.Properties;
import java.util.concurrent.TimeUnit;

/**
 * @author Vlad Mihalcea
 */
@Ignore
public class ConnectionPoolCallTest extends DataSourceProviderIntegrationTest {

    private final Logger LOGGER = LoggerFactory.getLogger(getClass());

    private MetricRegistry metricRegistry = new MetricRegistry();

    private Timer timer = metricRegistry.timer("callTimer");

    private Slf4jReporter logReporter = Slf4jReporter
            .forRegistry(metricRegistry)
            .outputTo(LOGGER)
            .build();

    private int callCount = 1000;

    public ConnectionPoolCallTest(DataSourceProvider dataSourceProvider) {
        super(dataSourceProvider);
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
        for (int i = 0; i < callCount; i++) {
            long startNanos = System.nanoTime();
            try (Connection connection = dataSource.getConnection()) {
            }
            timer.update(System.nanoTime() - startNanos, TimeUnit.NANOSECONDS);
        }
        logReporter.report();
    }

    protected HikariDataSource poolingDataSource() {
        Properties properties = new Properties();
        properties.setProperty("dataSourceClassName", dataSourceProvider().dataSourceClassName().getName());
        properties.put("dataSourceProperties", dataSourceProvider().dataSourceProperties());
        //properties.setProperty("minimumPoolSize", String.valueOf(1));
        properties.setProperty("maximumPoolSize", String.valueOf(3));
        properties.setProperty("connectionTimeout", String.valueOf(5000));
        return new HikariDataSource(new HikariConfig(properties));
    }
}
