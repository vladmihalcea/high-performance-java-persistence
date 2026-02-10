package com.vladmihalcea.hpjp.jdbc.caching;

import com.codahale.metrics.MetricRegistry;
import com.codahale.metrics.Slf4jReporter;
import com.codahale.metrics.Timer;
import com.microsoft.sqlserver.jdbc.SQLServerDataSource;
import com.vladmihalcea.hpjp.util.AbstractSQLServerIntegrationTest;
import com.vladmihalcea.hpjp.util.providers.SQLServerDataSourceProvider;
import jakarta.persistence.*;
import org.junit.jupiter.api.Test;

import javax.sql.DataSource;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;
import java.util.stream.LongStream;

import static org.junit.Assert.*;

/**
 * @author Vlad Mihalcea
 */
public class SQLServerStatementCacheTest extends AbstractSQLServerIntegrationTest {

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

    public static final String INSERT_POST = "insert into post (title) values (?)";

    private MetricRegistry metricRegistry = new MetricRegistry();

    private Slf4jReporter logReporter = Slf4jReporter
        .forRegistry(metricRegistry)
        .outputTo(LOGGER)
        .build();

    private Timer statementExecutionTimer = metricRegistry.timer("statementExecutionTimer");

    private ThreadLocalRandom random = ThreadLocalRandom.current();

    private boolean enableStatementCaching = false;

    private int cacheSize = enableStatementCaching ? getPostCount() : 0;

    @Override
    protected CachingSQLServerDataSourceProvider dataSourceProvider() {
        CachingSQLServerDataSourceProvider dataSourceProvider = new CachingSQLServerDataSourceProvider(cacheSize);
        return dataSourceProvider;
    }

    @Override
    protected Class<?>[] entities() {
        return new Class[] {
            Post.class
        };
    }

    protected int getPostCount() {
        return 1000;
    }

    protected int getPostCommentCount() {
        return 5;
    }

    protected long getRunNanos() {
        return TimeUnit.SECONDS.toNanos(60);
    }

    @Override
    protected boolean proxyDataSource() {
        return false;
    }

    @Test
    public void testInsertStatementCaching() {
        if(!ENABLE_LONG_RUNNING_TESTS) {
            return;
        }
        long ttlNanos = System.nanoTime() + getRunNanos();
        while (System.nanoTime() < ttlNanos) {
            doInJDBC(connection -> {
                int postCount = getPostCount();
                for (int i = 0; i < postCount; i++) {
                    if (System.nanoTime() > ttlNanos) {
                        return;
                    }
                    long startNanos = System.nanoTime();
                    if (i % 2 == 0) {
                        try (
                            PreparedStatement postStatement = connection.prepareStatement("""
                                INSERT INTO Posts (
                                    Title,
                                    CreatedOn,
                                    UpdatedOn,
                                    Category,
                                    Tags,
                                    Score
                                )
                                VALUES (?, ?, ?, ?, ?, ?)
                                """)) {
                            int index;
                            index = 0;
                            postStatement.setString(++index, String.format("Post no. %1$d", i));
                            postStatement.setTimestamp(++index, Timestamp.valueOf(LocalDateTime.now()));
                            postStatement.setTimestamp(++index, Timestamp.valueOf(LocalDateTime.now()));
                            postStatement.setString(++index, String.format("Category no. %1$d", i));
                            postStatement.setString(++index, String.format("Tags no. %1$d", i));
                            postStatement.setInt(++index, 0);
                            postStatement.executeUpdate();
                            statementExecutionTimer.update(System.nanoTime() - startNanos, TimeUnit.NANOSECONDS);
                        } catch (SQLException e) {
                            fail(e.getMessage());
                        }
                    } else {
                        try (
                            PreparedStatement postStatement = connection.prepareStatement("""
                                INSERT INTO Posts (
                                    Title,
                                    CreatedOn,
                                    Category,
                                    Tags
                                )
                                VALUES (?, ?, ?, ?)
                                """)) {
                            int index;
                            index = 0;
                            postStatement.setString(++index, String.format("Post no. %1$d", i));
                            postStatement.setTimestamp(++index, Timestamp.valueOf(LocalDateTime.now()));
                            postStatement.setString(++index, String.format("Category no. %1$d", i));
                            postStatement.setString(++index, String.format("Tags no. %1$d", i));
                            postStatement.executeUpdate();
                            statementExecutionTimer.update(System.nanoTime() - startNanos, TimeUnit.NANOSECONDS);
                        } catch (SQLException e) {
                            fail(e.getMessage());
                        }
                    }
                }
                connection.commit();
            });
        }
        LOGGER.info("Running with the statement cache [{}] an a cache size of [{}]", enableStatementCaching, cacheSize);
        logReporter.report();
    }

    @Test
    public void testStatementCaching() {
        if(!ENABLE_LONG_RUNNING_TESTS) {
            return;
        }
        doInJDBC(connection -> {
            try (
                PreparedStatement postStatement = connection.prepareStatement(INSERT_POST)) {
                int postCount = getPostCount();

                int index;

                for (int i = 0; i < postCount; i++) {
                    index = 0;
                    postStatement.setString(++index, String.format("Post no. %1$d", i));
                    postStatement.setInt(++index, 0);
                    postStatement.setLong(++index, i);
                    postStatement.executeUpdate();
                }
            } catch (SQLException e) {
                fail(e.getMessage());
            }
        });

        long ttlNanos = System.nanoTime() + getRunNanos();
        while (System.nanoTime() < ttlNanos) {
            doInJDBC(connection -> {
                for (int i = 0; i < 50; i++) {
                    if(System.nanoTime() > ttlNanos) {
                        return;
                    }
                    List<Long> ids = LongStream.rangeClosed(1, random.nextLong(2, 10)).boxed().toList();
                    StringBuilder inClauseBuilder = new StringBuilder();
                    ids.forEach(aLong -> {
                        inClauseBuilder.append(inClauseBuilder.isEmpty() ? "(" : ",");
                        inClauseBuilder.append("?");
                    });
                    inClauseBuilder.append(")");
                    long startNanos = System.nanoTime();
                    try (PreparedStatement statement = connection.prepareStatement(String.format("""
                        select p.title
                        from post p
                        where p.id in %s
                        """, inClauseBuilder))) {
                        for (int j = 0; j < ids.size(); j++) {
                            statement.setLong(j + 1, ids.get(j));
                        }
                        try (ResultSet resultSet = statement.executeQuery()) {
                            assertTrue(resultSet.next());
                        }
                        statementExecutionTimer.update(System.nanoTime() - startNanos, TimeUnit.NANOSECONDS);
                    } catch (SQLException e) {
                        fail(e.getMessage());
                    }
                }
            });
        }
        LOGGER.info("Running with the statement cache [{}] an a cache size of [{}]", enableStatementCaching, cacheSize);
        logReporter.report();
    }

    @Test
    public void testStatementCachingWithInQueries() {
        if(!ENABLE_LONG_RUNNING_TESTS) {
            return;
        }
        doInJDBC(connection -> {
            try (
                PreparedStatement postStatement = connection.prepareStatement(INSERT_POST)) {
                int postCount = getPostCount();

                int index;

                for (int i = 0; i < postCount; i++) {
                    index = 0;
                    postStatement.setString(++index, String.format("Post no. %1$d", i));
                    postStatement.setInt(++index, 0);
                    postStatement.setLong(++index, i);
                    postStatement.executeUpdate();
                }
            } catch (SQLException e) {
                fail(e.getMessage());
            }
        });

        doInJPA(entityManager -> {
            //Warm-up
            for (int i = 0; i < getPostCount(); i++) {
                executeInQuery(entityManager);
            }
            long ttlNanos = System.nanoTime() + getRunNanos();
            while (System.nanoTime() < ttlNanos) {
                long startNanos = System.nanoTime();
                executeInQuery(entityManager);
                statementExecutionTimer.update(System.nanoTime() - startNanos, TimeUnit.NANOSECONDS);
            }
        });
        LOGGER.info("Running with the statement cache [{}] an a cache size of [{}]", enableStatementCaching, cacheSize);
        logReporter.report();
    }

    private List<String> executeInQuery(EntityManager entityManager) {
        List<Long> ids = LongStream.rangeClosed(1, random.nextLong(2, getPostCount())).boxed().toList();
        List<String> titles = entityManager.createQuery("""
            select p.title
            from Post p
            where p.id in (:ids)
            """)
        .setParameter("ids", ids)
        .getResultList();
        assertEquals(titles.size(), ids.size());
        return titles;
    }

    @Entity(name = "Post")
    @Table(name = "Posts")
    public static class Post {

        @Id
        @GeneratedValue(strategy = GenerationType.IDENTITY)
        @Column(name = "PostId")
        private Long id;

        @Column(name = "Title")
        private String title;

        @Column(name = "CreatedOn")
        private LocalDateTime createdOn;

        @Column(name = "UpdatedOn")
        private LocalDateTime updatedOn;

        @Column(name = "Category")
        private String category;

        @Column(name = "Tags")
        private String tags;

        @Column(name = "Score")
        private int score;

        public Long getId() {
            return id;
        }

        public void setId(Long id) {
            this.id = id;
        }

        public String getTitle() {
            return title;
        }

        public void setTitle(String title) {
            this.title = title;
        }
    }
}
