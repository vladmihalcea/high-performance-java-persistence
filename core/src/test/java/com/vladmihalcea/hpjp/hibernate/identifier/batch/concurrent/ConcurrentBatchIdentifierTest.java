package com.vladmihalcea.hpjp.hibernate.identifier.batch.concurrent;

import com.codahale.metrics.MetricRegistry;
import com.codahale.metrics.Slf4jReporter;
import com.codahale.metrics.Timer;
import com.vladmihalcea.hpjp.hibernate.identifier.batch.concurrent.providers.IdentityPostEntityProvider;
import com.vladmihalcea.hpjp.hibernate.identifier.batch.concurrent.providers.PostEntityProvider;
import com.vladmihalcea.hpjp.hibernate.identifier.batch.concurrent.providers.SequencePostEntityProvider;
import com.vladmihalcea.hpjp.hibernate.identifier.batch.concurrent.providers.TablePostEntityProvider;
import com.vladmihalcea.hpjp.util.AbstractTest;
import com.vladmihalcea.hpjp.util.providers.DataSourceProvider;
import com.vladmihalcea.hpjp.util.providers.MySQLDataSourceProvider;
import com.vladmihalcea.hpjp.util.providers.PostgreSQLDataSourceProvider;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.Parameter;
import org.junit.jupiter.params.ParameterizedClass;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import java.util.concurrent.*;
import java.util.stream.Stream;

@ParameterizedClass
@MethodSource("parameters")
public class ConcurrentBatchIdentifierTest<T> extends AbstractTest {

    @Parameter(0)
    private DataSourceProvider dataSourceProvider;
    @Parameter(1)
    private PostEntityProvider entityProvider;
    @Parameter(2)
    private int threadCount;

    private int insertCount = 100;
    private int executionCount = 50;

    private ExecutorService executorService;

    private MetricRegistry metricRegistry = new MetricRegistry();

    private Timer timer = metricRegistry.timer(getClass().getSimpleName());

    private Slf4jReporter logReporter = Slf4jReporter
            .forRegistry(metricRegistry)
            .outputTo(LOGGER)
            .build();

    public static Stream<Arguments> parameters() {
        MySQLDataSourceProvider mySQLDataSourceProvider = new MySQLDataSourceProvider();
        PostgreSQLDataSourceProvider postgreSQLDataSourceProvider = new PostgreSQLDataSourceProvider();
        IdentityPostEntityProvider identityPostEntityProvider = new IdentityPostEntityProvider();
        SequencePostEntityProvider sequencePostEntityProvider = new SequencePostEntityProvider();
        TablePostEntityProvider tablePostEntityProvider = new TablePostEntityProvider();

        return Stream.of(
            Arguments.of(mySQLDataSourceProvider, tablePostEntityProvider, 1),
            Arguments.of(mySQLDataSourceProvider, tablePostEntityProvider, 2),
            Arguments.of(mySQLDataSourceProvider, tablePostEntityProvider, 4),
            Arguments.of(mySQLDataSourceProvider, tablePostEntityProvider, 8),
            Arguments.of(mySQLDataSourceProvider, tablePostEntityProvider, 16),
            Arguments.of(mySQLDataSourceProvider, identityPostEntityProvider, 1),
            Arguments.of(mySQLDataSourceProvider, identityPostEntityProvider, 2),
            Arguments.of(mySQLDataSourceProvider, identityPostEntityProvider, 4),
            Arguments.of(mySQLDataSourceProvider, identityPostEntityProvider, 8),
            Arguments.of(mySQLDataSourceProvider, identityPostEntityProvider, 16),

            Arguments.of(postgreSQLDataSourceProvider, tablePostEntityProvider, 1),
            Arguments.of(postgreSQLDataSourceProvider, tablePostEntityProvider, 2),
            Arguments.of(postgreSQLDataSourceProvider, tablePostEntityProvider, 4),
            Arguments.of(postgreSQLDataSourceProvider, tablePostEntityProvider, 8),
            Arguments.of(postgreSQLDataSourceProvider, tablePostEntityProvider, 16),
            Arguments.of(postgreSQLDataSourceProvider, sequencePostEntityProvider, 1),
            Arguments.of(postgreSQLDataSourceProvider, sequencePostEntityProvider, 2),
            Arguments.of(postgreSQLDataSourceProvider, sequencePostEntityProvider, 4),
            Arguments.of(postgreSQLDataSourceProvider, sequencePostEntityProvider, 8),
            Arguments.of(postgreSQLDataSourceProvider, sequencePostEntityProvider, 16)
        );
    }

    @Override
    protected DataSourceProvider dataSourceProvider() {
        return dataSourceProvider;
    }

    @Test
    @Disabled
    public void testIdentifierGenerator() throws InterruptedException, ExecutionException {
        LOGGER.debug("testIdentifierGenerator, database: {}, entityProvider: {}, threadCount: {}", dataSourceProvider.database(), entityProvider.getClass().getSimpleName(), threadCount);
        //warming-up
        doInJPA(entityManager -> {
            for (int i = 0; i < insertCount * executionCount; i++) {
                entityManager.persist(entityProvider.newPost());
            }
        });

        List<Worker> workers = new ArrayList<>(threadCount);
        for (int i = 0; i < threadCount; i++) {
            workers.add(new Worker());
        }
        List<Future<Boolean>> futures = executorService.invokeAll(workers);
        for(Future<Boolean> future : futures) {
            future.get();
        }
        logReporter.report();
    }

    public class Worker implements Callable<Boolean> {
        @Override
        public Boolean call() throws Exception {
            for (int i = 0; i < executionCount; i++) {
                doInJPA(entityManager -> {
                    long startNanos = System.nanoTime();
                    for (int j = 0; j < insertCount; j++) {
                        entityManager.persist(entityProvider.newPost());
                    }
                    entityManager.flush();
                    timer.update(System.nanoTime() - startNanos, TimeUnit.NANOSECONDS);
                    entityManager.clear();
                });
            }
            return true;
        }
    }

    @Override
    protected Class<?>[] entities() {
        return entityProvider.entities();
    }

    @Override
    protected Properties properties() {
        Properties properties = super.properties();
        properties.put("hibernate.order_inserts", "true");
        properties.put("hibernate.order_updates", "true");
        properties.put("hibernate.jdbc.batch_size", String.valueOf(insertCount));
        return properties;
    }

    @Override
    protected boolean connectionPooling() {
        return true;
    }
}
