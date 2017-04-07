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
 * ResultSetColumnSizeTest - Test result set column size
 *
 * @author Vlad Mihalcea
 */
public class ResultSetColumnSizeTest extends DataSourceProviderIntegrationTest {

    public static final String INSERT_POST = "insert into post (title, version, id) values (?, ?, ?)";

    public static final String INSERT_POST_COMMENT = "insert into post_comment (post_id, review, version, id) values (?, ?, ?, ?)";

    public static final String INSERT_POST_DETAILS= "insert into post_details (id, created_on, version) values (?, ?, ?)";

    public static final String SELECT_ALL =
            "select *  " +
                    "from post_comment pc " +
                    "inner join post p on p.id = pc.post_id " +
                    "inner join post_details pd on p.id = pd.id ";

    public static final String SELECT_ID =
            "select pc.version  " +
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

    public ResultSetColumnSizeTest(DataSourceProvider dataSourceProvider) {
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
            LOGGER.info("{} supports CLOSE_CURSORS_AT_COMMIT {}",
                    dataSourceProvider().database(),
                    connection.getMetaData().supportsResultSetHoldability(ResultSet.CLOSE_CURSORS_AT_COMMIT)
            );

            LOGGER.info("{} supports HOLD_CURSORS_OVER_COMMIT {}",
                    dataSourceProvider().database(),
                    connection.getMetaData().supportsResultSetHoldability(ResultSet.HOLD_CURSORS_OVER_COMMIT)
            );

            try (
                    PreparedStatement postStatement = connection.prepareStatement(INSERT_POST);
                    PreparedStatement postCommentStatement = connection.prepareStatement(INSERT_POST_COMMENT);
                    PreparedStatement postDetailsStatement = connection.prepareStatement(INSERT_POST_DETAILS);
            ) {

                if (postStatement.getResultSetHoldability() == ResultSet.CLOSE_CURSORS_AT_COMMIT) {
                    LOGGER.info("{} default holdability CLOSE_CURSORS_AT_COMMIT",
                            dataSourceProvider().database()
                    );
                } else if (postStatement.getResultSetHoldability() == ResultSet.HOLD_CURSORS_OVER_COMMIT) {
                    LOGGER.info("{} default holdability HOLD_CURSORS_OVER_COMMIT",
                            dataSourceProvider().database()
                    );
                } else {
                    fail();
                }

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
    public void testSelectAll() {
        testInternal(SELECT_ALL);
    }

    @Test
    public void testProjection() {
        testInternal(SELECT_ID);
    }

    public void testInternal(String sql) {
        doInJDBC(connection -> {
            for (int i = 0; i < runCount(); i++) {
                try (PreparedStatement statement = connection.prepareStatement(
                        sql
                )) {
                    statement.execute();
                    long startNanos = System.nanoTime();
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
        LOGGER.info("{} Result Set statement {}", dataSourceProvider().database(), sql);
        logReporter.report();
    }

    private int runCount() {
        return 10;
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
