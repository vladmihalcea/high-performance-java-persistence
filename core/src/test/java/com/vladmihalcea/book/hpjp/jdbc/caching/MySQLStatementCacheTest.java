package com.vladmihalcea.book.hpjp.jdbc.caching;

import com.codahale.metrics.MetricRegistry;
import com.codahale.metrics.Slf4jReporter;
import com.codahale.metrics.Timer;
import com.vladmihalcea.book.hpjp.util.DataSourceProviderIntegrationTest;
import com.vladmihalcea.book.hpjp.util.providers.DataSourceProvider;
import com.vladmihalcea.book.hpjp.util.providers.MySQLDataSourceProvider;
import com.vladmihalcea.book.hpjp.util.providers.entity.BlogEntityProvider;
import com.vladmihalcea.book.hpjp.util.providers.entity.BlogEntityProvider.Post;
import com.vladmihalcea.book.hpjp.util.providers.entity.BlogEntityProvider.PostComment;
import com.vladmihalcea.book.hpjp.util.providers.entity.BlogEntityProvider.PostDetails;
import org.hibernate.annotations.QueryHints;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runners.Parameterized;

import jakarta.persistence.EntityManager;
import java.sql.*;
import java.util.Date;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;

/**
 * @author Vlad Mihalcea
 */
public class MySQLStatementCacheTest extends DataSourceProviderIntegrationTest {

    private BlogEntityProvider entityProvider = new BlogEntityProvider();

    private MetricRegistry metricRegistry = new MetricRegistry();

    private Slf4jReporter logReporter = Slf4jReporter
        .forRegistry(metricRegistry)
        .outputTo(LOGGER)
        .build();

    private Timer findByIdTimer = metricRegistry.timer("findByIdTimer");

    private Timer flushTimer = metricRegistry.timer("flushTimer");

    private Timer query1Timer = metricRegistry.timer("query1Timer");

    private Timer query2Timer = metricRegistry.timer("query2Timer");

    public MySQLStatementCacheTest(DataSourceProvider dataSourceProvider) {
        super(dataSourceProvider);
    }

    @Parameterized.Parameters
    public static Collection<DataSourceProvider[]> rdbmsDataSourceProvider() {
        List<DataSourceProvider[]> providers = new ArrayList<>();

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
    protected void additionalProperties(Properties properties) {
        properties.put("hibernate.generate_statistics", Boolean.FALSE.toString());
        properties.put("hibernate.jdbc.batch_size", "50");
        properties.put("hibernate.order_inserts", "true");
        properties.put("hibernate.order_updates", "true");
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
    protected boolean proxyDataSource() {
        return false;
    }

    public int getRunCount() {
        return 5000;
    }

    protected int getRunMillis() {
        return 5 * 60 * 1000;
    }

    protected int getPostCount() {
        return 1000;
    }

    protected int getPostCommentCount() {
        return 5;
    }

    @Override
    public void afterInit() {
        int postCommentCount = getPostCommentCount();

        doInJPA(entityManager -> {
            for (long postId = 0; postId < getPostCount(); postId++) {
                Post post = new Post();
                post.setId(postId);
                post.setTitle(String.format("Post no. %1$d", postId));
                entityManager.persist(post);

                PostDetails details = new PostDetails();
                details.setId(postId);
                details.setCreatedBy("Vlad Mihalcea");
                details.setCreatedOn(new Date(System.currentTimeMillis()));
                details.setPost(post);
                entityManager.persist(details);

                for (int j = 0; j < postCommentCount; j++) {
                    PostComment comment = new PostComment();
                    comment.setId((postCommentCount * postId) + j);
                    comment.setReview(String.format("Post comment %1$d", j));
                    comment.setPost(post);

                    entityManager.persist(comment);
                }
            }
        });
    }

    @Test
    @Ignore
    public void testStatementCachingJPA() {
        long ttlMillis = System.currentTimeMillis() + getRunMillis();
        AtomicInteger transactionCount = new AtomicInteger();
        while (System.currentTimeMillis() < ttlMillis) {
            transactionCount.incrementAndGet();
            doInJPA(_entityManager -> {
                executeWithTiming(
                    _entityManager,
                    findByIdTimer,
                    (EntityManager entityManager) -> {
                        Post post = entityManager.find(Post.class, randomId());
                        PostDetails details = entityManager.find(PostDetails.class, randomId());
                        PostComment comment = entityManager.find(PostComment.class, randomId());

                        long millis = System.currentTimeMillis();
                        post.setTitle(String.format("Post no. %1$d", millis));
                        details.setCreatedOn(new Date(millis));
                        comment.setReview(
                            String.format("Post comment - %1$d", millis)
                        );
                    }
                );

                executeWithTiming(
                    _entityManager,
                    flushTimer,
                    EntityManager::flush
                );

                executeWithTiming(
                    _entityManager,
                    query1Timer,
                    (EntityManager entityManager) -> entityManager.createQuery("""
                        select p
                        from Post p
                        join fetch p.details pd
                        where p.id > :id
                        order by pd.id desc
                        """)
                    .setParameter("id", randomId())
                    .setMaxResults(5)
                    .setHint(QueryHints.FETCH_SIZE, Integer.MIN_VALUE)
                    .getResultStream()
                    .close()
                );

                executeWithTiming(
                    _entityManager,
                    query2Timer,
                    (EntityManager entityManager) -> entityManager.createQuery("""
                        select pc
                        from PostComment pc
                        join fetch pc.post p
                        where p.id > :postId 
                        order by p.id asc
                        """)
                    .setParameter("postId", randomId())
                    .setMaxResults(5)
                    .setHint(QueryHints.FETCH_SIZE, Integer.MIN_VALUE)
                    .getResultStream()
                    .close()
                );
            });
        }

        LOGGER.info(
            "MySQL connection settings: {}, throughput {} tps",
            dataSourceProvider(),
            transactionCount.get()
        );
        logReporter.report();
    }

    private void executeWithTiming(
        EntityManager entityManager,
        Timer timer,
        Consumer<EntityManager> consumer
    ) {
        long startNanos = System.nanoTime();
        consumer.accept(entityManager);
        timer.update(System.nanoTime() - startNanos, TimeUnit.NANOSECONDS);
    }

    private Long randomId() {
        return (long) (Math.random() * getPostCount());
    }

    @Test
    @Ignore
    public void testStatementCachingJDBC() {
        for (long i = 1; i <= getRunCount(); i++) {
            doInJDBC(connection -> {
                for (int j = 0; j < 5; j++) {
                    long startNanos = System.nanoTime();
                    try (PreparedStatement statement = connection.prepareStatement("""                   
                        SELECT p.title, pd.created_on, pc_c.comment_count
                        FROM post p
                        LEFT JOIN post_details pd ON p.id = pd.id
                        JOIN (
                            SELECT pc.post_id AS post_id, count(pc.id) AS comment_count
                            FROM post_comment pc
                            GROUP BY pc.post_id
                            ORDER BY pc.post_id DESC
                            LIMIT ?
                        ) pc_c ON p.id = pc_c.post_id
                        """)) {

                        statement.setInt(1, 5);
                        statement.executeQuery();
                    }
                    query1Timer.update(System.nanoTime() - startNanos, TimeUnit.NANOSECONDS);
                }
            });
        }

        LOGGER.info("MySQL connection settings: {}", dataSourceProvider());
        logReporter.report();
    }

    private void printServerSidePreparedStatementCount(Connection connection) throws SQLException {
        try (Statement psCountStatement = connection.createStatement();
             ResultSet psCountResultSet = psCountStatement.executeQuery("SHOW SESSION STATUS LIKE 'Prepared_stmt_count'")) {
            psCountResultSet.next();
            String statusName = psCountResultSet.getString(1);
            int statusValue = psCountResultSet.getInt(2);
            LOGGER.info("MySQL session status: {}, value: {}", statusName, statusValue);
        }
    }
}
