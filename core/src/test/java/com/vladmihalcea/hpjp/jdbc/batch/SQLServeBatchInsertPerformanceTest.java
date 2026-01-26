package com.vladmihalcea.hpjp.jdbc.batch;

import com.codahale.metrics.MetricRegistry;
import com.codahale.metrics.Slf4jReporter;
import com.codahale.metrics.Timer;
import com.vladmihalcea.hpjp.util.AbstractSQLServerIntegrationTest;
import com.vladmihalcea.hpjp.util.providers.DataSourceProvider;
import com.vladmihalcea.hpjp.util.providers.SQLServerDataSourceProvider;
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
 * @author Vlad Mihalcea
 */
@RunWith(Parameterized.class)
public class SQLServeBatchInsertPerformanceTest extends AbstractSQLServerIntegrationTest {

    private final BlogEntityProvider entityProvider = new BlogEntityProvider();

    private MetricRegistry metricRegistry = new MetricRegistry();

    private Slf4jReporter logReporter = Slf4jReporter
        .forRegistry(metricRegistry)
        .outputTo(LOGGER)
        .build();

    private Timer timer = metricRegistry.timer("batchInsertTimer");

    private boolean useJdbcBatchInsert;

    public SQLServeBatchInsertPerformanceTest(boolean useJdbcBatchInsert) {
        this.useJdbcBatchInsert = useJdbcBatchInsert;
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
        return ((SQLServerDataSourceProvider) super.dataSourceProvider())
            .setUseBulkCopyForBatchInsert(true)
            .setSendStringParametersAsUnicode(true);
    }

    @Test
    public void testInsert() {
        if (!ENABLE_LONG_RUNNING_TESTS) {
            return;
        }
        long ttlMillis = System.currentTimeMillis() + getRunMillis();

        final AtomicInteger postIdHolder = new AtomicInteger();

        int postCount = getPostCount();

        while (System.currentTimeMillis() < ttlMillis) {
            AtomicInteger postStatementCount = new AtomicInteger();

            doInJDBC(connection -> {
                long startNanos = System.nanoTime();
                try (PreparedStatement postStatement = connection.prepareStatement("INSERT INTO post (id, title, version) VALUES (?, ?, ?)")) {
                    for (int i = 0; i < postCount; i++) {
                        int index = 0;
                        int postId = postIdHolder.incrementAndGet();
                        postStatement.setLong(++index, postId);
                        postStatement.setString(++index, String.format("Post no. %1$d", postId));
                        postStatement.setInt(++index, 0);
                        executeStatement(postStatement, postStatementCount);
                    }
                    if (useJdbcBatchInsert) {
                        postStatement.executeBatch();
                    }
                } catch (SQLException e) {
                    fail(e.getMessage());
                }

                timer.update(System.nanoTime() - startNanos, TimeUnit.NANOSECONDS);
            });
        }
        Long postRecordCount = doInJPA(entityManager -> {
            return entityManager.createQuery("select count(p) from Post p", Long.class).getSingleResult();
        });
        LOGGER.info("Test SQL Server batch insert is {}", useJdbcBatchInsert);
        LOGGER.info(
            "{}, we inserted {} post table records",
            useJdbcBatchInsert ? "With JDBC Batch Inserts mode enabled" : "Without JDBC Batch Inserts (default mode)",
            postRecordCount
        );
        logReporter.report();
    }

    private void executeStatement(PreparedStatement statement, AtomicInteger statementCount) throws SQLException {
        int count = statementCount.incrementAndGet();
        if (useJdbcBatchInsert) {
            statement.addBatch();
            if (count % getBatchSize() == 0) {
                statement.executeBatch();
            }
        } else {
            statement.execute();
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
