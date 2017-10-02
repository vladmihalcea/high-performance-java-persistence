package com.vladmihalcea.book.hpjp.hibernate.statistics;

import com.vladmihalcea.book.hpjp.util.AbstractTest;
import com.vladmihalcea.book.hpjp.util.providers.entity.BlogEntityProvider;
import org.hibernate.cfg.AvailableSettings;
import org.hibernate.engine.jdbc.connections.internal.DatasourceConnectionProviderImpl;
import org.hibernate.resource.jdbc.spi.PhysicalConnectionHandlingMode;
import org.hibernate.stat.internal.StatisticsInitiator;
import org.junit.Test;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.Properties;
import java.util.concurrent.atomic.AtomicBoolean;

import static com.vladmihalcea.book.hpjp.util.providers.entity.BlogEntityProvider.Post;
import static org.junit.Assert.assertEquals;

/**
 * @author Vlad Mihalcea
 */
public class ConnectionStatisticsTest extends AbstractTest {

    @Override
    protected Class<?>[] entities() {
        return BlogEntityProvider.INSTANCE.entities();
    }

    private final AtomicBoolean autoCommit = new AtomicBoolean(true);

    private class ThreadLocalDatasourceConnectionProvider
            extends DatasourceConnectionProviderImpl {

        public ThreadLocalDatasourceConnectionProvider(final DataSource dataSource) {
            setDataSource(dataSource);
        }

        final ThreadLocal<Connection> connectionHolder = new ThreadLocal<>();

        @Override
        public Connection getConnection()
                throws SQLException {
            Connection connection = connectionHolder.get();
            if(connection == null) {
                connection = super.getConnection();
                if (!autoCommit.get()) {
                    connection.setAutoCommit(false);
                }
                connectionHolder.set(connection);
            }
            return connection;
        }

        @Override
        public void closeConnection(Connection connection)
                throws SQLException {
            if (autoCommit.get()) {
                super.closeConnection(connection);
                connectionHolder.remove();
            }
        }
    }

    protected void additionalProperties(Properties properties) {
        DataSource actualDataSource = (DataSource) properties
            .get(AvailableSettings.DATASOURCE);

        properties.put(
            AvailableSettings.CONNECTION_PROVIDER,
            new ThreadLocalDatasourceConnectionProvider(actualDataSource)
        );

        properties.put(
            AvailableSettings.CONNECTION_HANDLING,
            PhysicalConnectionHandlingMode.DELAYED_ACQUISITION_AND_RELEASE_AFTER_STATEMENT
        );

        properties.put(
            AvailableSettings.CONNECTION_PROVIDER_DISABLES_AUTOCOMMIT,
            Boolean.TRUE.toString()
        );

        properties.put(
            AvailableSettings.GENERATE_STATISTICS,
            Boolean.TRUE.toString()
        );

        properties.put(
            StatisticsInitiator.STATS_BUILDER,
            TransactionStatisticsFactory.class.getName()
        );
    }

    @Test
    public void test() {
        int iterations = 5;

        for (long i = 0; i < iterations; i++) {
            autoCommit.set(false);
            final Long id = i + 1;

            doInJPA(entityManager -> {
                try {
                    Post post = new Post();
                    post.setId(id);
                    post.setTitle(
                        String.format(
                            "High-Performance Java Persistence, Part %d", id
                        )
                    );
                    entityManager.persist(post);

                    Number postCount = entityManager.createQuery(
                        "select count(*) from Post", Number.class)
                    .getSingleResult();

                    assertEquals((long) id, postCount.longValue());
                }
                finally {
                    autoCommit.set(true);
                }
            });
        }
    }
}
