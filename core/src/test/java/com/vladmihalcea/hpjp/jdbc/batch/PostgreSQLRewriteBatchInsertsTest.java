package com.vladmihalcea.hpjp.jdbc.batch;

import com.codahale.metrics.MetricRegistry;
import com.codahale.metrics.Slf4jReporter;
import com.codahale.metrics.Timer;
import com.vladmihalcea.hpjp.util.AbstractPostgreSQLIntegrationTest;
import com.vladmihalcea.hpjp.util.providers.DataSourceProvider;
import com.vladmihalcea.hpjp.util.providers.PostgreSQLDataSourceProvider;
import com.vladmihalcea.hpjp.util.providers.entity.BlogEntityProvider;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.Assert.fail;

/**
 * PostgreSQLRewriteBatchInsertsTest - Test PostgreSQL JDBC Statement batching with and w/o reWriteBatchedInserts
 *
 * @author Vlad Mihalcea
 */
@RunWith(Parameterized.class)
public class PostgreSQLRewriteBatchInsertsTest extends AbstractPostgreSQLIntegrationTest {

    private final BlogEntityProvider entityProvider = new BlogEntityProvider();

    private MetricRegistry metricRegistry = new MetricRegistry();

    private Slf4jReporter logReporter = Slf4jReporter
        .forRegistry(metricRegistry)
        .outputTo(LOGGER)
        .build();

    private Timer timer = metricRegistry.timer("batchInsertTimer");

    private boolean reWriteBatchedInserts;

    public PostgreSQLRewriteBatchInsertsTest(boolean reWriteBatchedInserts) {
        this.reWriteBatchedInserts = reWriteBatchedInserts;
    }

    @Parameterized.Parameters
    public static Collection<Boolean[]> rdbmsDataSourceProvider() {
        List<Boolean[]> providers = new ArrayList<>();
        providers.add(new Boolean[]{Boolean.FALSE});
        providers.add(new Boolean[]{Boolean.TRUE});
        return providers;
    }

    @Override
    protected Class<?>[] entities() {
        return entityProvider.entities();
    }

    @Override
    protected DataSourceProvider dataSourceProvider() {
        return ((PostgreSQLDataSourceProvider) super.dataSourceProvider())
            .setReWriteBatchedInserts(reWriteBatchedInserts);
    }

    @Test
    public void testInsert() {
        if (!ENABLE_LONG_RUNNING_TESTS) {
            return;
        }
        long ttlMillis = System.currentTimeMillis() + getRunMillis();

        final AtomicInteger postIdHolder = new AtomicInteger();

        while (System.currentTimeMillis() < ttlMillis) {
            doInJDBC(connection -> {
                long startNanos = System.nanoTime();

                AtomicInteger postStatementCount = new AtomicInteger();

                try (PreparedStatement postStatement = connection.prepareStatement("insert into post (title, version, id) values (?, ?, ?)")) {
                    int postCount = getPostCount();

                    for (int i = 0; i < postCount; i++) {
                        int index = 0;
                        int postId = postIdHolder.incrementAndGet();
                        postStatement.setString(++index, String.format("Post no. %1$d", postId));
                        postStatement.setInt(++index, 0);
                        postStatement.setLong(++index, postId);
                        executeStatement(postStatement, postStatementCount);
                    }
                    postStatement.executeBatch();
                } catch (SQLException e) {
                    fail(e.getMessage());
                }

                timer.update(System.nanoTime() - startNanos, TimeUnit.NANOSECONDS);
            });
        }
        LOGGER.info("Test PostgreSQL batch insert with reWriteBatchedInserts={}", reWriteBatchedInserts);
        logReporter.report();
        LOGGER.info(
            "Test PostgreSQL batch insert with reWriteBatchedInserts={} took=[{}] ms",
            reWriteBatchedInserts,
            timer.getSnapshot().get99thPercentile()
        );
    }

    private void executeStatement(PreparedStatement statement, AtomicInteger statementCount) throws SQLException {
        statement.addBatch();
        int count = statementCount.incrementAndGet();
        if (count % getBatchSize() == 0) {
            statement.executeBatch();
        }
    }

    protected int getPostCount() {
        return 5000;
    }

    protected int getBatchSize() {
        return 100 * 10;
    }

    protected int getRunMillis() {
        return 60 * 1000;
    }
}
