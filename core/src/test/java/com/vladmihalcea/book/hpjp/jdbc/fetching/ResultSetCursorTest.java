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
import java.sql.Timestamp;
import java.util.concurrent.TimeUnit;

import static org.junit.Assert.fail;

/**
 * ResultSetCursorTest - Test result set cursor
 *
 * @author Vlad Mihalcea
 */
public class ResultSetCursorTest extends DataSourceProviderIntegrationTest {

    public static final String INSERT_POST = "insert into post (title, version, id) values (?, ?, ?)";

    public static final String INSERT_POST_COMMENT = "insert into post_comment (post_id, review, version, id) values (?, ?, ?, ?)";

    public static final String INSERT_POST_DETAILS= "insert into post_details (id, created_on, version) values (?, ?, ?)";

    public static final String SELECT_ALL =
            "select *  " +
                    "from post_comment pc " +
                    "inner join post p on p.id = pc.post_id " +
                    "inner join post_details pd on p.id = pd.id ";

    private MetricRegistry metricRegistry = new MetricRegistry();

    private Timer timer = metricRegistry.timer("callSequence");

    private Slf4jReporter logReporter = Slf4jReporter
            .forRegistry(metricRegistry)
            .outputTo(LOGGER)
            .build();

    private BlogEntityProvider entityProvider = new BlogEntityProvider();

    public ResultSetCursorTest(DataSourceProvider dataSourceProvider) {
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
            LOGGER.info("{} supports TYPE_FORWARD_ONLY {}, CONCUR_READ_ONLY {}",
                    dataSourceProvider().database(),
                    connection.getMetaData().supportsResultSetType(ResultSet.TYPE_FORWARD_ONLY),
                    connection.getMetaData().supportsResultSetConcurrency(ResultSet.TYPE_FORWARD_ONLY, ResultSet.CONCUR_READ_ONLY)
            );

            LOGGER.info("{} supports TYPE_FORWARD_ONLY {}, CONCUR_UPDATABLE {}",
                    dataSourceProvider().database(),
                    connection.getMetaData().supportsResultSetType(ResultSet.TYPE_FORWARD_ONLY),
                    connection.getMetaData().supportsResultSetConcurrency(ResultSet.TYPE_FORWARD_ONLY, ResultSet.CONCUR_UPDATABLE)
            );

            LOGGER.info("{} supports TYPE_SCROLL_INSENSITIVE {}, CONCUR_READ_ONLY {}",
                    dataSourceProvider().database(),
                    connection.getMetaData().supportsResultSetType(ResultSet.TYPE_SCROLL_INSENSITIVE),
                    connection.getMetaData().supportsResultSetConcurrency(ResultSet.TYPE_SCROLL_INSENSITIVE, ResultSet.CONCUR_READ_ONLY)
            );

            LOGGER.info("{} supports TYPE_SCROLL_INSENSITIVE {}, CONCUR_UPDATABLE {}",
                    dataSourceProvider().database(),
                    connection.getMetaData().supportsResultSetType(ResultSet.TYPE_SCROLL_INSENSITIVE),
                    connection.getMetaData().supportsResultSetConcurrency(ResultSet.TYPE_SCROLL_INSENSITIVE, ResultSet.CONCUR_UPDATABLE)
            );

            LOGGER.info("{} supports TYPE_SCROLL_SENSITIVE {}, CONCUR_READ_ONLY {}",
                    dataSourceProvider().database(),
                    connection.getMetaData().supportsResultSetType(ResultSet.TYPE_SCROLL_SENSITIVE),
                    connection.getMetaData().supportsResultSetConcurrency(ResultSet.TYPE_SCROLL_SENSITIVE, ResultSet.CONCUR_READ_ONLY)
            );

            LOGGER.info("{} supports TYPE_SCROLL_SENSITIVE {}, CONCUR_UPDATABLE {}",
                    dataSourceProvider().database(),
                    connection.getMetaData().supportsResultSetType(ResultSet.TYPE_SCROLL_SENSITIVE),
                    connection.getMetaData().supportsResultSetConcurrency(ResultSet.TYPE_SCROLL_SENSITIVE, ResultSet.CONCUR_UPDATABLE)
            );

            try (
                    PreparedStatement postStatement = connection.prepareStatement(INSERT_POST);
                    PreparedStatement postCommentStatement = connection.prepareStatement(INSERT_POST_COMMENT);
                    PreparedStatement postDetailsStatement = connection.prepareStatement(INSERT_POST_DETAILS);
            ) {
                int postCount = getPostCount();
                int postCommentCount = getPostCommentCount();

                int index;

                for (int i = 0; i < postCount; i++) {
                    if (i > 0 && i % 100 == 0) {
                        postStatement.executeBatch();
                        postDetailsStatement.executeBatch();
                    }
                    index = 0;
                    postStatement.setString(++index, String.format("Post no. %1$d", i));
                    postStatement.setInt(++index, 0);
                    postStatement.setLong(++index, i);
                    postStatement.addBatch();

                    index = 0;
                    postDetailsStatement.setInt(++index, i);
                    postDetailsStatement.setTimestamp(++index, new Timestamp(System.currentTimeMillis()));
                    postDetailsStatement.setInt(++index, 0);
                    postDetailsStatement.addBatch();
                }
                postStatement.executeBatch();
                postDetailsStatement.executeBatch();

                for (int i = 0; i < postCount; i++) {
                    for (int j = 0; j < postCommentCount; j++) {
                        index = 0;
                        postCommentStatement.setLong(++index, i);
                        postCommentStatement.setString(++index, String.format("Post comment %1$d", j));
                        postCommentStatement.setInt(++index, (int) (Math.random() * 1000));
                        postCommentStatement.setLong(++index, (postCommentCount * i) + j);
                        postCommentStatement.addBatch();
                        if (j % 100 == 0) {
                            postCommentStatement.executeBatch();
                        }
                    }
                }
                postCommentStatement.executeBatch();
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
        doInJDBC(connection -> {
            for (int i = 0; i < runCount(); i++) {
                long startNanos = System.nanoTime();
                try (PreparedStatement statement = connection.prepareStatement(
                        SELECT_ALL
                        , resultSetType, resultSetConcurrency)) {
                    statement.execute();
                    ResultSet resultSet = statement.getResultSet();
                    while (resultSet.next()) {
                        Object[] values = new Object[resultSet.getMetaData().getColumnCount()];
                        for (int j = 0; j < values.length; j++) {
                            values[j] = resultSet.getObject(j + 1);
                        }
                    }
                    timer.update(System.nanoTime() - startNanos, TimeUnit.NANOSECONDS);
                } catch (SQLException e) {
                    fail(e.getMessage());
                }
            }
        });
        LOGGER.info("{} Result Set Type {} and Concurrency {}",
                dataSourceProvider().database(),
                resultSetType == ResultSet.TYPE_FORWARD_ONLY ? "ResultSet.TYPE_FORWARD_ONLY" : "ResultSet.TYPE_SCROLL_SENSITIVE",
                resultSetConcurrency == ResultSet.CONCUR_READ_ONLY ? "ResultSet.CONCUR_READ_ONLY" : "ResultSet.CONCUR_UPDATABLE");
        logReporter.report();
    }

    private int runCount() {
        return 1;
    }

    protected int getPostCount() {
        return 100;
    }

    protected int getPostCommentCount() {
        return 10;
    }

    @Override
    protected boolean proxyDataSource() {
        return false;
    }
}
