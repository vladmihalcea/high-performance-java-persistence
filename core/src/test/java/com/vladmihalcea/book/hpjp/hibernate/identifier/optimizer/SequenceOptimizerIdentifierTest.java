package com.vladmihalcea.book.hpjp.hibernate.identifier.optimizer;

import com.codahale.metrics.MetricRegistry;
import com.codahale.metrics.Slf4jReporter;
import com.codahale.metrics.Timer;
import com.vladmihalcea.book.hpjp.hibernate.identifier.optimizer.providers.*;
import com.vladmihalcea.book.hpjp.util.AbstractTest;
import com.vladmihalcea.book.hpjp.util.providers.DataSourceProvider;
import com.vladmihalcea.book.hpjp.util.providers.OracleDataSourceProvider;
import com.vladmihalcea.book.hpjp.util.providers.PostgreSQLDataSourceProvider;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Properties;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;

@RunWith(Parameterized.class)
public class SequenceOptimizerIdentifierTest extends AbstractTest {

    private final DataSourceProvider dataSourceProvider;
    private final PostEntityProvider entityProvider;

    private int insertCount = 50;
    private int executionCount = 50;

    private MetricRegistry metricRegistry = new MetricRegistry();

    private Timer timer = metricRegistry.timer(getClass().getSimpleName());

    private Slf4jReporter logReporter = Slf4jReporter
            .forRegistry(metricRegistry)
            .outputTo(LOGGER)
            .build();


    public SequenceOptimizerIdentifierTest(DataSourceProvider dataSourceProvider, PostEntityProvider entityProvider) {
        this.dataSourceProvider = dataSourceProvider;
        this.entityProvider = entityProvider;
    }

    @Parameterized.Parameters
    public static Collection<Object[]> dataProvider() {
        OracleDataSourceProvider oracleDataSourceProvider = new OracleDataSourceProvider();
        PostgreSQLDataSourceProvider postgreSQLDataSourceProvider = new PostgreSQLDataSourceProvider();

        Sequence1PostEntityProvider sequence1PostEntityProvider = new Sequence1PostEntityProvider();
        Sequence5PostEntityProvider sequence5PostEntityProvider = new Sequence5PostEntityProvider();
        Sequence10PostEntityProvider sequence10PostEntityProvider = new Sequence10PostEntityProvider();
        Sequence50PostEntityProvider sequence50PostEntityProvider = new Sequence50PostEntityProvider();

        Table1PostEntityProvider table1PostEntityProvider = new Table1PostEntityProvider();
        Table5PostEntityProvider table5PostEntityProvider = new Table5PostEntityProvider();
        Table10PostEntityProvider table10PostEntityProvider = new Table10PostEntityProvider();
        Table50PostEntityProvider table50PostEntityProvider = new Table50PostEntityProvider();

        List<Object[]> providers = new ArrayList<>();
        providers.add(new Object[]{postgreSQLDataSourceProvider, sequence1PostEntityProvider});
        providers.add(new Object[]{postgreSQLDataSourceProvider, sequence5PostEntityProvider});
        providers.add(new Object[]{postgreSQLDataSourceProvider, sequence10PostEntityProvider});
        providers.add(new Object[]{postgreSQLDataSourceProvider, sequence50PostEntityProvider});
        providers.add(new Object[]{postgreSQLDataSourceProvider, table1PostEntityProvider});
        providers.add(new Object[]{postgreSQLDataSourceProvider, table5PostEntityProvider});
        providers.add(new Object[]{postgreSQLDataSourceProvider, table10PostEntityProvider});
        providers.add(new Object[]{postgreSQLDataSourceProvider, table50PostEntityProvider});
        return providers;
    }

    @Override
    protected DataSourceProvider dataSourceProvider() {
        return dataSourceProvider;
    }

    @Test
    public void testIdentifierGenerator() throws InterruptedException, ExecutionException {
        LOGGER.debug("testIdentifierGenerator, database: {}, entityProvider: {}", dataSourceProvider.database(), entityProvider.getClass().getSimpleName());
        //warming-up
        doInJPA(entityManager -> {
            for (int i = 0; i < insertCount; i++) {
                entityManager.persist(entityProvider.newPost());
            }
        });
        doInJPA(entityManager -> {
            for (int i = 0; i < executionCount; i++) {
                for (int j = 0; j < insertCount; j++) {
                    long startNanos = System.nanoTime();
                    entityManager.persist(entityProvider.newPost());
                    timer.update(System.nanoTime() - startNanos, TimeUnit.NANOSECONDS);
                }
                entityManager.flush();
                entityManager.clear();
            }
        });
        logReporter.report();
        sleep(100);
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
