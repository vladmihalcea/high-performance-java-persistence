package com.vladmihalcea.hpjp.jdbc.fetching;

import com.codahale.metrics.MetricRegistry;
import com.codahale.metrics.Slf4jReporter;
import com.codahale.metrics.Timer;
import com.vladmihalcea.hpjp.util.DatabaseProviderIntegrationTest;
import com.vladmihalcea.hpjp.util.providers.DataSourceProvider;
import com.vladmihalcea.hpjp.util.providers.Database;
import com.vladmihalcea.hpjp.util.providers.LegacyOracleDialect;
import com.vladmihalcea.hpjp.util.providers.OracleDataSourceProvider;
import com.vladmihalcea.hpjp.util.providers.entity.BlogEntityProvider;
import org.hibernate.dialect.pagination.LimitHandler;
import org.hibernate.query.spi.Limit;
import org.junit.Test;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.concurrent.TimeUnit;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

/**
 * ResultSetLimitTest - Test limiting result set vs fetching and discarding rows
 *
 * @author Vlad Mihalcea
 */
public class ResultSetLimitTest extends DatabaseProviderIntegrationTest {

    public static final String INSERT_POST = "insert into post (title, version, id) values (?, ?, ?)";

    public static final String INSERT_POST_COMMENT = "insert into post_comment (post_id, review, version, id) values (?, ?, ?, ?)";

    public static final String SELECT_POST_COMMENT = """
        SELECT pc.id AS pc_id, p.title AS p_title
        FROM post_comment pc
        INNER JOIN post p ON p.id = pc.post_id
        ORDER BY pc_id
        """;

    private BlogEntityProvider entityProvider = new BlogEntityProvider();

    private MetricRegistry metricRegistry = new MetricRegistry();

    private Slf4jReporter logReporter = Slf4jReporter
        .forRegistry(metricRegistry)
        .outputTo(LOGGER)
        .build();

    private Timer noLimitTimer = metricRegistry.timer("noLimitTimer");
    private Timer limitTimer = metricRegistry.timer("limitTimer");
    private Timer maxSizeTimer = metricRegistry.timer("maxSizeTimer");

    public ResultSetLimitTest(Database database) {
        super(database);
    }

    @Override
    protected Class<?>[] entities() {
        return entityProvider.entities();
    }

    @Override
    protected DataSourceProvider dataSourceProvider() {
        if(database() == Database.ORACLE) {
            return new OracleDataSourceProvider() {
                @Override
                public String hibernateDialect() {
                    return LegacyOracleDialect.class.getName();
                }
            };
        }
        return super.dataSourceProvider();
    }

    public void afterInit() {
        if(!ENABLE_LONG_RUNNING_TESTS) {
            return;
        }
        doInJDBC(connection -> {
            try (
                PreparedStatement postStatement = connection.prepareStatement(INSERT_POST);
                PreparedStatement postCommentStatement = connection.prepareStatement(INSERT_POST_COMMENT);
            ) {
                int postCount = getPostCount();
                int postCommentCount = getPostCommentCount();

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
        if(database() != Database.MYSQL) {
            executeStatement("CREATE INDEX idx_post_comment_post_id ON post_comment (post_id)");
            if(database() == Database.POSTGRESQL) {
                executeStatement("VACUUM FULL ANALYZE");
            }
        }
    }

    @Test
    public void test() {
        if(!ENABLE_LONG_RUNNING_TESTS) {
            return;
        }
        doInJDBC(connection -> {
            try (PreparedStatement statement = connection.prepareStatement(
                SELECT_POST_COMMENT
            )) {
                for (int i = 0; i < runCount() / 10; i++) {
                    noLimit(statement);
                }
                for (int i = 0; i < runCount(); i++) {
                    long startNanos = System.nanoTime();
                    noLimit(statement);
                    noLimitTimer.update((System.nanoTime() - startNanos), TimeUnit.NANOSECONDS);
                }
            } catch (SQLException e) {
                fail(e.getMessage());
            }
        });

        doInJDBC(connection -> {
            final Limit rowSelection = new Limit();
            rowSelection.setMaxRows(getMaxRows());
            LimitHandler limitHandler = dialect().getLimitHandler();
            String limitStatement = limitHandler.processSql(SELECT_POST_COMMENT, rowSelection);
            try (PreparedStatement statement = connection.prepareStatement(limitStatement)) {
                for (int i = 0; i < runCount() / 10; i++) {
                    limit(statement, limitHandler, rowSelection);
                }
                for (int i = 0; i < runCount(); i++) {
                    long startNanos = System.nanoTime();
                    limit(statement, limitHandler, rowSelection);
                    limitTimer.update((System.nanoTime() - startNanos), TimeUnit.NANOSECONDS);
                }
            } catch (SQLException e) {
                fail(e.getMessage());
            }
        });

        doInJDBC(connection -> {
            try (PreparedStatement statement = connection.prepareStatement(
                SELECT_POST_COMMENT
            )) {
                for (int i = 0; i < runCount() / 10; i++) {
                    maxSize(statement);
                }
                for (int i = 0; i < runCount(); i++) {
                    long startNanos = System.nanoTime();
                    maxSize(statement);
                    maxSizeTimer.update((System.nanoTime() - startNanos), TimeUnit.NANOSECONDS);
                }
            } catch (SQLException e) {
                fail(e.getMessage());
            }
        });
        LOGGER.info("{} results:", database());
        logReporter.report();
    }

    private void noLimit(PreparedStatement statement) throws SQLException {
        statement.setFetchSize(100);
        statement.execute();
        ResultSet resultSet = statement.getResultSet();
        int count = 0;
        while (resultSet.next()) {
            resultSet.getLong(1);
            count++;
        }
        assertEquals(getPostCount() * getPostCommentCount(), count);
    }

    public void limit(PreparedStatement statement, LimitHandler limitHandler, Limit rowSelection) throws SQLException {
        limitHandler.bindLimitParametersAtEndOfQuery(rowSelection, statement, 1);
        statement.setInt(1, getMaxRows());
        statement.execute();
        int count = 0;
        ResultSet resultSet = statement.getResultSet();
        while (resultSet.next()) {
            resultSet.getLong(1);
            count++;
        }
        assertEquals(getMaxRows(), count);
    }

    public void maxSize(PreparedStatement statement) throws SQLException {
        statement.setMaxRows(getMaxRows());
        ResultSet resultSet = statement.executeQuery();
        int count = 0;
        while (resultSet.next()) {
            resultSet.getLong(1);
            count++;
        }
        assertEquals(getMaxRows(), count);
    }

    protected int getPostCount() {
        return 10_000;
    }

    protected int getPostCommentCount() {
        return 10;
    }

    protected int getMaxRows() {
        return 100;
    }

    private int runCount() {
        return 1000;
    }

    @Override
    protected boolean proxyDataSource() {
        return false;
    }
}
