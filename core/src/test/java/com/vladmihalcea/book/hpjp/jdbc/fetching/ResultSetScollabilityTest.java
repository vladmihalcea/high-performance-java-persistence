package com.vladmihalcea.book.hpjp.jdbc.fetching;

import com.codahale.metrics.MetricRegistry;
import com.codahale.metrics.Slf4jReporter;
import com.codahale.metrics.Timer;
import com.vladmihalcea.book.hpjp.util.DataSourceProviderIntegrationTest;
import com.vladmihalcea.book.hpjp.util.providers.entity.BlogEntityProvider;
import com.vladmihalcea.book.hpjp.util.providers.DataSourceProvider;

import org.junit.Test;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.concurrent.TimeUnit;

import static org.junit.Assert.fail;

/**
 * ResultSetScollabilityTest - Test result set scrollability
 *
 * @author Vlad Mihalcea
 */
public class ResultSetScollabilityTest extends DataSourceProviderIntegrationTest {

    public static final String INSERT_POST = "insert into post (title, version, id) values (?, ?, ?)";

    private MetricRegistry metricRegistry = new MetricRegistry();

    private Timer timer = metricRegistry.timer("callSequence");

    private Slf4jReporter logReporter = Slf4jReporter
            .forRegistry(metricRegistry)
            .outputTo(LOGGER)
            .build();

    private BlogEntityProvider entityProvider = new BlogEntityProvider();

    public ResultSetScollabilityTest(DataSourceProvider dataSourceProvider) {
        super(dataSourceProvider);
    }

    @Override
    protected Class<?>[] entities() {
        return entityProvider.entities();
    }

    @Override
    public void init() {
        super.init();
        doInJDBC(connection -> {
            LOGGER.info("{} supports TYPE_FORWARD_ONLY {}", dataSourceProvider().database(), connection.getMetaData().supportsResultSetType(ResultSet.TYPE_FORWARD_ONLY));
            LOGGER.info("{} supports TYPE_SCROLL_INSENSITIVE {}", dataSourceProvider().database(), connection.getMetaData().supportsResultSetType(ResultSet.TYPE_SCROLL_INSENSITIVE));
            LOGGER.info("{} supports TYPE_SCROLL_SENSITIVE {}", dataSourceProvider().database(), connection.getMetaData().supportsResultSetType(ResultSet.TYPE_SCROLL_SENSITIVE));

            try (
                    PreparedStatement postStatement = connection.prepareStatement(INSERT_POST);
            ) {
                int postCount = getPostCount();

                int index;

                for (int i = 0; i < postCount; i++) {
                    if (i > 0 && i % 100 == 0) {
                        postStatement.executeBatch();
                    }
                    index = 0;
                    postStatement.setString(++index, String.format("Post no. %1$d", i));
                    postStatement.setInt(++index, 0);
                    postStatement.setLong(++index, i);
                    postStatement.addBatch();
                }
                postStatement.executeBatch();
            } catch (SQLException e) {
                fail(e.getMessage());
            }
        });
    }

    @Test
    public void testDefault() {
        testInternal(ResultSet.TYPE_FORWARD_ONLY, ResultSet.CONCUR_READ_ONLY);
    }

    @Test
    public void testCursor() {
        testInternal(ResultSet.TYPE_SCROLL_SENSITIVE, ResultSet.CONCUR_UPDATABLE);
    }

    public void testInternal(int resultSetType, int resultSetConcurrency) {
        long startNanos = System.nanoTime();
        doInJDBC(connection -> {
            try (PreparedStatement statement = connection.prepareStatement(
                    "select * " +
                            "from Post p "
                    , resultSetType, resultSetConcurrency)) {
                statement.execute();
                ResultSet resultSet = statement.getResultSet();
                Object[] values = new Object[resultSet.getMetaData().getColumnCount()];
                for (int i = 0; i < getPostCount(); i++) {
                    resultSet.absolute(i + 1);
                    for (int j = 0; j < values.length; j++) {
                        values[j] = resultSet.getObject(j + 1);
                    }
                }
                timer.update(System.nanoTime() - startNanos, TimeUnit.NANOSECONDS);
            } catch (SQLException e) {
                LOGGER.error("Failed {}", e);
            }
        });
        LOGGER.info("{} Result Set Type {} and Concurrency {}",
                dataSourceProvider().database(),
                resultSetType == ResultSet.TYPE_FORWARD_ONLY ? "ResultSet.TYPE_FORWARD_ONLY" : "ResultSet.TYPE_SCROLL_SENSITIVE",
                resultSetConcurrency == ResultSet.CONCUR_READ_ONLY ? "ResultSet.CONCUR_READ_ONLY" : "ResultSet.CONCUR_UPDATABLE");
        logReporter.report();
    }

    protected int getPostCount() {
        return 100;
    }

    @Override
    protected boolean proxyDataSource() {
        return false;
    }
}
