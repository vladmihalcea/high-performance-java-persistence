package com.vladmihalcea.hpjp.hibernate.identifier.optimizer;

import com.codahale.metrics.MetricRegistry;
import com.codahale.metrics.Slf4jReporter;
import com.codahale.metrics.Timer;
import com.vladmihalcea.hpjp.hibernate.identifier.optimizer.providers.*;
import com.vladmihalcea.hpjp.util.AbstractTest;
import com.vladmihalcea.hpjp.util.providers.DataSourceProvider;
import com.vladmihalcea.hpjp.util.providers.OracleDataSourceProvider;
import com.vladmihalcea.hpjp.util.providers.PostgreSQLDataSourceProvider;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.Parameter;
import org.junit.jupiter.params.ParameterizedClass;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.Properties;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.stream.Stream;

@ParameterizedClass
@MethodSource("parameters")
public class SequenceOptimizerIdentifierTest extends AbstractTest {

    @Parameter(0)
    private DataSourceProvider dataSourceProvider;
    @Parameter(1)
    private PostEntityProvider entityProvider;

    private int insertCount = 50;
    private int executionCount = 50;

    private MetricRegistry metricRegistry = new MetricRegistry();

    private Timer timer = metricRegistry.timer(getClass().getSimpleName());

    private Slf4jReporter logReporter = Slf4jReporter
            .forRegistry(metricRegistry)
            .outputTo(LOGGER)
            .build();

    public static Stream<Arguments> parameters() {
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

        return Stream.of(
            Arguments.of(postgreSQLDataSourceProvider, sequence1PostEntityProvider),
            Arguments.of(postgreSQLDataSourceProvider, sequence5PostEntityProvider),
            Arguments.of(postgreSQLDataSourceProvider, sequence10PostEntityProvider),
            Arguments.of(postgreSQLDataSourceProvider, sequence50PostEntityProvider),
            Arguments.of(postgreSQLDataSourceProvider, table1PostEntityProvider),
            Arguments.of(postgreSQLDataSourceProvider, table5PostEntityProvider),
            Arguments.of(postgreSQLDataSourceProvider, table10PostEntityProvider),
            Arguments.of(postgreSQLDataSourceProvider, table50PostEntityProvider)
        );
    }

    @Override
    protected DataSourceProvider dataSourceProvider() {
        return dataSourceProvider;
    }

    @Test
    @Disabled
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
