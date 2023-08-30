package com.vladmihalcea.hpjp.jdbc.caching;

import com.vladmihalcea.hpjp.util.AbstractPostgreSQLIntegrationTest;
import com.vladmihalcea.hpjp.util.ReflectionUtils;
import com.zaxxer.hikari.HikariConfig;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import org.junit.Test;
import org.postgresql.core.CachedQuery;
import org.postgresql.jdbc.PgStatement;

import javax.sql.DataSource;
import java.sql.PreparedStatement;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.*;

/**
 * @author Vlad Mihalcea
 */
public class PostgreSQLStatementCachePoolingTest extends AbstractPostgreSQLIntegrationTest {

    public static final String INSERT_POST = "insert into post (title, id) values (?, ?)";
    
    @Override
    protected Class<?>[] entities() {
        return new Class[] {Post.class};
    }

    @Override
    protected void additionalProperties(Properties properties) {
        properties.put("hibernate.jdbc.batch_size", "100");
        properties.put("hibernate.order_inserts", "true");
    }

    @Override
    public void afterInit() {
        doInJPA(entityManager -> {
            for (long i = 1; i <= postCount(); i++) {
                entityManager.persist(
                    new Post()
                        .setId(i)
                        .setTitle(
                            String.format(
                                "High-Performance Java Persistence, page %d", i
                            )
                        )
                );
            }
        });
    }

    @Test
    public void testStatementCaching() throws ExecutionException, InterruptedException {
        ThreadLocalRandom random = ThreadLocalRandom.current();
        ExecutorService executorService = Executors.newFixedThreadPool(4);
        int statementExecutionCount = 100;
        List<Future<?>> futures = new ArrayList<>(statementExecutionCount);
        ConcurrentMap<CachedQuery, CachedQueryStats> cachedQueryStatsMap = new ConcurrentHashMap<>();

        for (long i = 1; i <= statementExecutionCount; i++) {
            futures.add(
                executorService.submit(() -> {
                    doInJDBC(connection -> {
                        try (PreparedStatement statement = connection.prepareStatement(
                            "SELECT title FROM post WHERE id = ?"
                        )) {
                            PgStatement pgStatement = statement.unwrap(PgStatement.class);
                            CachedQuery cachedQuery = ReflectionUtils.getFieldValue(pgStatement, "preparedQuery");
                            CachedQueryStats cachedQueryStats = cachedQueryStatsMap.computeIfAbsent(cachedQuery, pgc -> new CachedQueryStats());
                            cachedQueryStats.incrementExecutionCount();
                            if(pgStatement.isUseServerPrepare()) {
                                cachedQueryStats.incrementPreparedExecutions();
                            } else {
                                cachedQueryStats.incrementUnpreparedExecutions();
                            }
                            statement.setLong(1, random.nextLong(postCount()));
                            statement.executeQuery();
                        }
                    });
                })
            );
        }

        for(Future<?> future : futures) {
            future.get();
        }
        for(Map.Entry<CachedQuery, CachedQueryStats> statisticsMapEntry : cachedQueryStatsMap.entrySet()) {
            CachedQuery cachedQuery = statisticsMapEntry.getKey();
            CachedQueryStats cachedQueryStats = statisticsMapEntry.getValue();
            LOGGER.error("Statement [{}] stats: [{}]", cachedQuery, cachedQueryStats);
        }
    }

    protected int postCount() {
        return 100;
    }

    @Override
    protected boolean proxyDataSource() {
        return false;
    }

    @Override
    protected boolean connectionPooling() {
        return true;
    }

    @Override
    protected HikariConfig hikariConfig(DataSource dataSource) {
        HikariConfig hikariConfig = new HikariConfig();
        hikariConfig.setMaximumPoolSize(4);
        hikariConfig.setDataSource(dataSource);
        return hikariConfig;
    }

    @Entity(name = "Post")
    @Table(name = "post")
    public static class Post {

        @Id
        private Long id;

        private String title;

        public Long getId() {
            return id;
        }

        public Post setId(Long id) {
            this.id = id;
            return this;
        }

        public String getTitle() {
            return title;
        }

        public Post setTitle(String title) {
            this.title = title;
            return this;
        }
    }

    public static class CachedQueryStats {
        private long executionCount;
        private long unpreparedExecutions;
        private long preparedExecutions;

        public long executionCount() {
            return executionCount;
        }

        public long unpreparedExecutions() {
            return unpreparedExecutions;
        }

        public long preparedExecutions() {
            return preparedExecutions;
        }

        public void incrementExecutionCount() {
            executionCount++;
        }

        public void incrementUnpreparedExecutions() {
            unpreparedExecutions++;
        }

        public void incrementPreparedExecutions() {
            preparedExecutions++;
        }

        @Override
        public String toString() {
            return "executionCount=" + executionCount +
                   ", unpreparedExecutions=" + unpreparedExecutions +
                   ", preparedExecutions=" + preparedExecutions;
        }
    }
}
