package com.vladmihalcea.book.hpjp.hibernate.identifier.batch.concurrent;

import com.codahale.metrics.MetricRegistry;
import com.codahale.metrics.Slf4jReporter;
import com.codahale.metrics.Timer;
import com.vladmihalcea.book.hpjp.hibernate.identifier.batch.concurrent.providers.IdentityPostEntityProvider;
import com.vladmihalcea.book.hpjp.hibernate.identifier.batch.concurrent.providers.PostEntityProvider;
import com.vladmihalcea.book.hpjp.hibernate.identifier.batch.concurrent.providers.TablePostEntityProvider;
import com.vladmihalcea.book.hpjp.util.AbstractMySQLIntegrationTest;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import javax.persistence.*;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Properties;
import java.util.concurrent.*;

@RunWith(Parameterized.class)
public class ConcurrentBatchIdentifierTest<T> extends AbstractMySQLIntegrationTest {

    private final PostEntityProvider entityProvider;
    private final int threadCount;

    private int insertCount = 100;
    private int executionCount = 10;

    private final ExecutorService executorService;

    private MetricRegistry metricRegistry = new MetricRegistry();

    private Timer timer = metricRegistry.timer(getClass().getSimpleName());

    private Slf4jReporter logReporter = Slf4jReporter
            .forRegistry(metricRegistry)
            .outputTo(LOGGER)
            .build();


    public ConcurrentBatchIdentifierTest(PostEntityProvider entityProvider, int threadCount) {
        this.entityProvider = entityProvider;
        this.threadCount = threadCount;
        executorService = Executors.newFixedThreadPool(threadCount);
    }

    @Parameterized.Parameters
    public static Collection<Object[]> dataProvider() {
        IdentityPostEntityProvider identityPostEntityProvider = new IdentityPostEntityProvider();
        TablePostEntityProvider tablePostEntityProvider = new TablePostEntityProvider();

        List<Object[]> providers = new ArrayList<>();
        providers.add(new Object[]{identityPostEntityProvider, 1});
        providers.add(new Object[]{identityPostEntityProvider, 2});
        providers.add(new Object[]{identityPostEntityProvider, 4});
        providers.add(new Object[]{identityPostEntityProvider, 8});
        providers.add(new Object[]{identityPostEntityProvider, 16});
        providers.add(new Object[]{tablePostEntityProvider, 1});
        providers.add(new Object[]{tablePostEntityProvider, 2});
        providers.add(new Object[]{tablePostEntityProvider, 4});
        providers.add(new Object[]{tablePostEntityProvider, 8});
        providers.add(new Object[]{tablePostEntityProvider, 16});
        return providers;
    }

    @Test
    public void testIdentifierGenerator() throws InterruptedException, ExecutionException {
        LOGGER.debug("testIdentifierGenerator, entityProvider: {}, threadCount: {}", entityProvider.getClass().getSimpleName(), threadCount);
        //warming-up
        doInJPA(entityManager -> {
            for (int i = 0; i < insertCount; i++) {
                entityManager.persist(entityProvider.newPost());
            }
        });

        List<Worker> workers = new ArrayList<>(threadCount);
        for (int i = 0; i < threadCount; i++) {
            workers.add(new Worker());
        }
        for (int i = 0; i < executionCount; i++) {
            long startNanos = System.nanoTime();
            for(Future<Boolean> future : executorService.invokeAll(workers)) {
                future.get();
            }
            timer.update(System.nanoTime() - startNanos, TimeUnit.NANOSECONDS);
        }
        logReporter.report();
    }

    public class Worker implements Callable<Boolean> {
        @Override
        public Boolean call() throws Exception {
            doInJPA(entityManager -> {
                for (int i = 0; i < insertCount; i++) {
                    entityManager.persist(entityProvider.newPost());
                }
            });
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
}
