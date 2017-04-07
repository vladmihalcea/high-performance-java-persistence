package com.vladmihalcea.book.hpjp.hibernate.identifier.batch.concurrent;

import com.codahale.metrics.MetricRegistry;
import com.codahale.metrics.Slf4jReporter;
import com.codahale.metrics.Timer;
import com.vladmihalcea.book.hpjp.hibernate.identifier.batch.concurrent.providers.IdentityPostEntityProvider;
import com.vladmihalcea.book.hpjp.hibernate.identifier.batch.concurrent.providers.PostEntityProvider;
import com.vladmihalcea.book.hpjp.hibernate.identifier.batch.concurrent.providers.SequencePostEntityProvider;
import com.vladmihalcea.book.hpjp.hibernate.identifier.batch.concurrent.providers.TablePostEntityProvider;
import com.vladmihalcea.book.hpjp.util.AbstractTest;
import com.vladmihalcea.book.hpjp.util.providers.DataSourceProvider;
import com.vladmihalcea.book.hpjp.util.providers.MySQLDataSourceProvider;
import com.vladmihalcea.book.hpjp.util.providers.PostgreSQLDataSourceProvider;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Properties;
import java.util.concurrent.*;

@RunWith(Parameterized.class)
public class ConcurrentBatchIdentifierTest<T> extends AbstractTest {

    private final DataSourceProvider dataSourceProvider;
    private final PostEntityProvider entityProvider;
    private final int threadCount;

    private int insertCount = 100;
    private int executionCount = 50;

    private final ExecutorService executorService;

    private MetricRegistry metricRegistry = new MetricRegistry();

    private Timer timer = metricRegistry.timer(getClass().getSimpleName());

    private Slf4jReporter logReporter = Slf4jReporter
            .forRegistry(metricRegistry)
            .outputTo(LOGGER)
            .build();


    public ConcurrentBatchIdentifierTest(DataSourceProvider dataSourceProvider, PostEntityProvider entityProvider, int threadCount) {
        this.dataSourceProvider = dataSourceProvider;
        this.entityProvider = entityProvider;
        this.threadCount = threadCount;
        executorService = Executors.newFixedThreadPool(threadCount);
    }

    @Parameterized.Parameters
    public static Collection<Object[]> dataProvider() {
        MySQLDataSourceProvider mySQLDataSourceProvider = new MySQLDataSourceProvider();
        PostgreSQLDataSourceProvider postgreSQLDataSourceProvider = new PostgreSQLDataSourceProvider();
        IdentityPostEntityProvider identityPostEntityProvider = new IdentityPostEntityProvider();
        SequencePostEntityProvider sequencePostEntityProvider = new SequencePostEntityProvider();
        TablePostEntityProvider tablePostEntityProvider = new TablePostEntityProvider();

        List<Object[]> providers = new ArrayList<>();
        providers.add(new Object[]{mySQLDataSourceProvider, tablePostEntityProvider, 1});
        providers.add(new Object[]{mySQLDataSourceProvider, tablePostEntityProvider, 2});
        providers.add(new Object[]{mySQLDataSourceProvider, tablePostEntityProvider, 4});
        providers.add(new Object[]{mySQLDataSourceProvider, tablePostEntityProvider, 8});
        providers.add(new Object[]{mySQLDataSourceProvider, tablePostEntityProvider, 16});
        providers.add(new Object[]{mySQLDataSourceProvider, identityPostEntityProvider, 1});
        providers.add(new Object[]{mySQLDataSourceProvider, identityPostEntityProvider, 2});
        providers.add(new Object[]{mySQLDataSourceProvider, identityPostEntityProvider, 4});
        providers.add(new Object[]{mySQLDataSourceProvider, identityPostEntityProvider, 8});
        providers.add(new Object[]{mySQLDataSourceProvider, identityPostEntityProvider, 16});

        providers.add(new Object[]{postgreSQLDataSourceProvider, tablePostEntityProvider, 1});
        providers.add(new Object[]{postgreSQLDataSourceProvider, tablePostEntityProvider, 2});
        providers.add(new Object[]{postgreSQLDataSourceProvider, tablePostEntityProvider, 4});
        providers.add(new Object[]{postgreSQLDataSourceProvider, tablePostEntityProvider, 8});
        providers.add(new Object[]{postgreSQLDataSourceProvider, tablePostEntityProvider, 16});
        providers.add(new Object[]{postgreSQLDataSourceProvider, sequencePostEntityProvider, 1});
        providers.add(new Object[]{postgreSQLDataSourceProvider, sequencePostEntityProvider, 2});
        providers.add(new Object[]{postgreSQLDataSourceProvider, sequencePostEntityProvider, 4});
        providers.add(new Object[]{postgreSQLDataSourceProvider, sequencePostEntityProvider, 8});
        providers.add(new Object[]{postgreSQLDataSourceProvider, sequencePostEntityProvider, 16});
        return providers;
    }

    @Override
    protected DataSourceProvider dataSourceProvider() {
        return dataSourceProvider;
    }

    @Test
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
