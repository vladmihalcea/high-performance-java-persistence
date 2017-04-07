package com.vladmihalcea.book.hpjp.jdbc.batch.generatedkeys.sequence;

import com.codahale.metrics.MetricRegistry;
import com.codahale.metrics.Slf4jReporter;
import com.codahale.metrics.Timer;
import com.vladmihalcea.book.hpjp.util.AbstractTest;
import com.vladmihalcea.book.hpjp.util.providers.entity.SequenceBatchEntityProvider;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.concurrent.TimeUnit;

/**
 * @author Vlad Mihalcea
 */
public abstract class AbstractSequenceCallTest extends AbstractTest {

    private final Logger LOGGER = LoggerFactory.getLogger(getClass());

    private MetricRegistry metricRegistry = new MetricRegistry();

    private Timer timer = metricRegistry.timer("callSequence");

    private Slf4jReporter logReporter = Slf4jReporter
            .forRegistry(metricRegistry)
            .outputTo(LOGGER)
            .build();

    private int ttl = 60;

    private SequenceBatchEntityProvider entityProvider = new SequenceBatchEntityProvider();

    public AbstractSequenceCallTest() {
        logReporter.start(ttl, TimeUnit.SECONDS);
    }

    @Override
    protected Class<?>[] entities() {
        return entityProvider.entities();
    }

    @Test
    public void testBatch() {
        doInJDBC(this::callSequence);
    }

    protected void callSequence(Connection connection) throws SQLException {
        LOGGER.info("{} callSequence", dataSourceProvider().database());

        try (Statement statement = connection.createStatement()) {
            long startNanos = System.nanoTime();
            while (TimeUnit.NANOSECONDS.toSeconds(System.nanoTime() - startNanos) < ttl + 1) {
                long startCall = System.nanoTime();
                try (ResultSet resultSet = statement.executeQuery(
                        callSequenceSyntax())) {
                    resultSet.next();
                    resultSet.getLong(1);
                }
                timer.update(System.nanoTime() - startCall, TimeUnit.NANOSECONDS);
            }
        }
    }

    protected abstract String callSequenceSyntax();
}
