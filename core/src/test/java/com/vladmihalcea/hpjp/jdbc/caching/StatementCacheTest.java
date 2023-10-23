package com.vladmihalcea.hpjp.jdbc.caching;

import com.codahale.metrics.MetricRegistry;
import com.codahale.metrics.Slf4jReporter;
import com.codahale.metrics.Timer;
import com.microsoft.sqlserver.jdbc.SQLServerDataSource;
import com.vladmihalcea.hpjp.util.DatabaseProviderIntegrationTest;
import com.vladmihalcea.hpjp.util.providers.*;
import com.vladmihalcea.hpjp.util.providers.entity.BlogEntityProvider;
import oracle.jdbc.pool.OracleDataSource;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runners.Parameterized;
import org.postgresql.ds.PGSimpleDataSource;

import javax.sql.DataSource;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Properties;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

/**
 * @author Vlad Mihalcea
 */
public class StatementCacheTest extends DatabaseProviderIntegrationTest {

    public static class CachingOracleDataSourceProvider extends OracleDataSourceProvider {
        private final int cacheSize;

        CachingOracleDataSourceProvider(int cacheSize) {
            this.cacheSize = cacheSize;
        }

        @Override
        public DataSource dataSource() {
            OracleDataSource dataSource = (OracleDataSource) super.dataSource();
            try {
                Properties connectionProperties = dataSource.getConnectionProperties();
                if(connectionProperties == null) {
                    connectionProperties = new Properties();
                }
                connectionProperties.put("oracle.jdbc.implicitStatementCacheSize", Integer.toString(cacheSize));
                dataSource.setConnectionProperties(connectionProperties);
            } catch (Exception e) {
                fail(e.getMessage());
            }
            return dataSource;
        }

        @Override
        public String toString() {
            return "CachingOracleDataSourceProvider{" +
                    "cacheSize=" + cacheSize +
                    '}';
        }
    }

    public static class CachingSQLServerDataSourceProvider extends SQLServerDataSourceProvider {
        private final int cacheSize;

        CachingSQLServerDataSourceProvider(int cacheSize) {
            this.cacheSize = cacheSize;
        }

        @Override
        public DataSource dataSource() {
            SQLServerDataSource dataSource = (SQLServerDataSource) super.dataSource();
            dataSource.setStatementPoolingCacheSize(cacheSize);
            if (cacheSize > 0) {
                dataSource.setDisableStatementPooling(false);
            }
            return dataSource;
        }

        @Override
        public String toString() {
            return "CachingSQLServerDataSourceProvider{" +
                    "cacheSize=" + cacheSize +
                    '}';
        }
    }

    public static class CachingPostgreSQLDataSourceProvider extends PostgreSQLDataSourceProvider {
        private final int cacheSize;

        CachingPostgreSQLDataSourceProvider(int cacheSize) {
            this.cacheSize = cacheSize;
        }

        @Override
        public DataSource dataSource() {
            PGSimpleDataSource dataSource = (PGSimpleDataSource) super.dataSource();
            dataSource.setPreparedStatementCacheQueries(cacheSize);
            return dataSource;
        }

        @Override
        public String toString() {
            return "CachingPostgreSQLDataSourceProvider{" +
                    "cacheSize=" + cacheSize +
                    '}';
        }
    }

    public static final String INSERT_POST = "insert into post (title, version, id) values (?, ?, ?)";

    public static final String INSERT_POST_COMMENT = "insert into post_comment (post_id, review, version, id) values (?, ?, ?, ?)";

    private BlogEntityProvider entityProvider = new BlogEntityProvider();

    private MetricRegistry metricRegistry = new MetricRegistry();

    private Slf4jReporter logReporter = Slf4jReporter
        .forRegistry(metricRegistry)
        .outputTo(LOGGER)
        .build();

    private Timer queryTimer = metricRegistry.timer("queryTimer");

    public StatementCacheTest(Database database) {
        super(database);
    }

    @Parameterized.Parameters
    public static Collection<DataSourceProvider[]> rdbmsDataSourceProvider() {
        List<DataSourceProvider[]> providers = new ArrayList<>();
        /*providers.add(new DataSourceProvider[]{
                new CachingOracleDataSourceProvider(1)
        });
        providers.add(new DataSourceProvider[]{
                new CachingOracleDataSourceProvider(0)
        });
        providers.add(new DataSourceProvider[]{
                new CachingSQLServerDataSourceProvider(1)
        });
        providers.add(new DataSourceProvider[]{
                new CachingSQLServerDataSourceProvider(0)
        });
        providers.add(new DataSourceProvider[]{
                new CachingPostgreSQLDataSourceProvider(1)
        });
        providers.add(new DataSourceProvider[]{
                new CachingPostgreSQLDataSourceProvider(0)
        });*/

        providers.add(new DataSourceProvider[]{
            new MySQLDataSourceProvider()
                .setUseServerPrepStmts(false)
                .setCachePrepStmts(false)
        });

        providers.add(new DataSourceProvider[]{
            new MySQLDataSourceProvider()
                .setUseServerPrepStmts(true)
                .setCachePrepStmts(false)
        });

        providers.add(new DataSourceProvider[]{
            new MySQLDataSourceProvider()
                .setUseServerPrepStmts(false)
                .setCachePrepStmts(true)
                .setPrepStmtCacheSqlLimit(2048)
        });

        providers.add(new DataSourceProvider[]{
            new MySQLDataSourceProvider()
                .setUseServerPrepStmts(true)
                .setCachePrepStmts(true)
                .setPrepStmtCacheSqlLimit(2048)
        });

        return providers;
    }

    @Override
    protected Class<?>[] entities() {
        return entityProvider.entities();
    }

    @Override
    protected boolean connectionPooling() {
        return true;
    }

    @Override
    public void afterInit() {
        doInJDBC(connection -> {
            try (
                    PreparedStatement postStatement = connection.prepareStatement(INSERT_POST);
                    PreparedStatement postCommentStatement = connection.prepareStatement(INSERT_POST_COMMENT);
            ) {
                int postCount = getPostCount();
                int postCommentCount = getPostCommentCount();

                int index;

                for (int i = 0; i < postCount; i++) {
                    index = 0;
                    postStatement.setString(++index, String.format("Post no. %1$d", i));
                    postStatement.setInt(++index, 0);
                    postStatement.setLong(++index, i);
                    postStatement.executeUpdate();
                }

                for (int i = 0; i < postCount; i++) {
                    for (int j = 0; j < postCommentCount; j++) {
                        index = 0;
                        postCommentStatement.setLong(++index, i);
                        postCommentStatement.setString(++index, String.format("Post comment %1$d", j));
                        postCommentStatement.setInt(++index, (int) (Math.random() * 1000));
                        postCommentStatement.setLong(++index, (postCommentCount * i) + j);
                        postCommentStatement.executeUpdate();
                    }
                }
            } catch (SQLException e) {
                fail(e.getMessage());
            }
        });
    }

    @Test
    @Ignore
    public void testMySQLStatementCaching() {
        if(dataSourceProvider().database() != Database.MYSQL) {
            return;
        }
        AtomicInteger queryCount = new AtomicInteger();
        doInJDBC(connection -> {
            long ttlNanos = System.nanoTime() + getRunNanos();
            while (System.nanoTime() < ttlNanos) {
                long startNanos = System.nanoTime();
                try (PreparedStatement statement = connection.prepareStatement("""
                    SELECT p.title, pd.created_on
                    FROM post p
                    LEFT JOIN post_details pd ON p.id = pd.id
                    WHERE EXISTS (
                        SELECT 1 FROM post_comment WHERE post_id = p.id
                    )
                    ORDER BY p.id
                    LIMIT ?
                    OFFSET ?
                    """
                )) {
                    statement.setInt(1, 1);
                    statement.setInt(2, 100);
                    try(ResultSet resultSet = statement.executeQuery()) {
                        queryCount.incrementAndGet();
                    } finally {
                        queryTimer.update(System.nanoTime() - startNanos, TimeUnit.NANOSECONDS);
                    }
                }
            }
        });
        LOGGER.info("When using {}, throughput is {} statements",
                dataSourceProvider(),
                queryCount.get()
        );
        logReporter.report();
    }

    @Test
    @Ignore
    public void testPostgreSQLStatementCaching() {
        if(dataSourceProvider().database() != Database.POSTGRESQL) {
            return;
        }
        AtomicInteger queryCount = new AtomicInteger();
        doInJDBC(connection -> {
            long ttlNanos = System.nanoTime() + getRunNanos();
            while (System.nanoTime() < ttlNanos) {
                long startNanos = System.nanoTime();
                try (PreparedStatement statement = connection.prepareStatement("""
                    SELECT p.title, pd.created_on
                    FROM post p
                    LEFT JOIN post_details pd ON p.id = pd.id
                    WHERE EXISTS (
                        SELECT 1 FROM post_comment WHERE post_id = p.id
                    )
                    ORDER BY p.id
                    LIMIT ?
                    OFFSET ?
                    """
                )) {
                    statement.setInt(1, 1);
                    statement.setInt(2, 100);
                    try(ResultSet resultSet = statement.executeQuery()) {
                        queryCount.incrementAndGet();
                    } finally {
                        queryTimer.update(System.nanoTime() - startNanos, TimeUnit.NANOSECONDS);
                    }
                } catch (SQLException e) {
                    fail(e.getMessage());
                }
            }
        });
        LOGGER.info("When using {}, throughput is {} statements",
            dataSourceProvider(),
            queryCount.get()
        );
        logReporter.report();
    }

    @Test
    @Ignore
    public void testStatementCaching2() {
        long ttlMillis = System.currentTimeMillis() + getRunNanos();
        AtomicLong counterHolder = new AtomicLong();
        doInJDBC(connection -> {
            long statementCount = 0;
            while (System.currentTimeMillis() < ttlMillis) {
                try (PreparedStatement statement = connection.prepareStatement("""
                    select p.title
                    from post p
                    where p.id = ?
                    """
                )) {
                    statement.setInt(1, (int) (Math.random() * getPostCount()));
                    try (ResultSet resultSet = statement.executeQuery()) {
                        assertTrue(resultSet.next());
                    }
                    statementCount++;
                } catch (SQLException e) {
                    fail(e.getMessage());
                }
            }
            counterHolder.set(statementCount);
        });
        LOGGER.info("When using {}, throughput is {} statements per minute",
                dataSourceProvider(),
                counterHolder.get());
    }

    protected int getPostCount() {
        return 1000;
    }

    protected int getPostCommentCount() {
        return 5;
    }

    protected long getRunNanos() {
        return TimeUnit.MINUTES.toNanos(5);
    }

    @Override
    protected boolean proxyDataSource() {
        return false;
    }
}
