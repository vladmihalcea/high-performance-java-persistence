package com.vladmihalcea.book.high_performance_java_persistence.jdbc.batch;

import com.vladmihalcea.book.high_performance_java_persistence.util.providers.BatchEntityProvider;
import com.vladmihalcea.book.high_performance_java_persistence.util.AbstractOracleXEIntegrationTest;
import oracle.jdbc.pool.OracleDataSource;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import javax.sql.DataSource;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.*;
import java.util.concurrent.TimeUnit;

import static org.junit.Assert.fail;

/**
 * BatchStatementTest - Test batching with Statements
 *
 * @author Vlad Mihalcea
 */
@RunWith(Parameterized.class)
public class OracleDefaultExecuteBatchPreparedStatementTest extends AbstractOracleXEIntegrationTest {

    public static final String INSERT_POST = "insert into post (title, version, id) values (?, ?, ?)";

    public static final String INSERT_POST_COMMENT = "insert into post_comment (post_id, review, version, id) values (?, ?, ?, ?)";

    private final int defaultExecuteBatch;

    private BatchEntityProvider entityProvider = new BatchEntityProvider();

    public OracleDefaultExecuteBatchPreparedStatementTest(int defaultExecuteBatch) {
        this.defaultExecuteBatch = defaultExecuteBatch;
    }

    @Parameterized.Parameters
    public static Collection<Integer[]> defaultExecuteBatches() {
        List<Integer[]> providers = new ArrayList<>();
        providers.add(new Integer[] {1});
        providers.add(new Integer[] {50});
        return providers;
    }

    @Override
    protected DataSourceProvider getDataSourceProvider() {
        return new OracleDataSourceProvider() {
            @Override
            public DataSource dataSource() {
                OracleDataSource dataSource = (OracleDataSource) super.dataSource();
                try {
                    Properties connectionProperties = dataSource.getConnectionProperties();
                    if(connectionProperties == null) {
                        connectionProperties = new Properties();
                    }
                    connectionProperties.setProperty("defaultExecuteBatch", String.valueOf(defaultExecuteBatch));
                    dataSource.setConnectionProperties(connectionProperties);
                } catch (SQLException e) {
                    fail(e.getMessage());
                }
                return dataSource;
            }
        };
    }

    @Override
    protected Class<?>[] entities() {
        return entityProvider.entities();
    }

    @Test
    public void testInsert() {
        LOGGER.info("Test batch insert for defaultExecuteBatch {}", defaultExecuteBatch);
        long startNanos = System.nanoTime();
        doInConnection(connection -> {
            try (
                PreparedStatement postStatement = connection.prepareStatement(INSERT_POST);
                PreparedStatement postCommentStatement = connection.prepareStatement(INSERT_POST_COMMENT);
            ) {
                int postCount = getPostCount();
                int postCommentCount = getPostCommentCount();

                int index;

                for(int i = 0; i < postCount; i++) {
                    index = 0;
                    postStatement.setString(++index, String.format("Post no. %1$d", i));
                    postStatement.setInt(++index, 0);
                    postStatement.setLong(++index, i);
                    postStatement.executeUpdate();
                }

                for(int i = 0; i < postCount; i++) {
                    for(int j = 0; j < postCommentCount; j++) {
                        index = 0;
                        postCommentStatement.setLong(++index, i);
                        postCommentStatement.setString(++index, String.format("Post comment %1$d", j));
                        postCommentStatement.setInt(++index, 0);
                        postCommentStatement.setLong(++index, (postCommentCount * i) + j);
                        postCommentStatement.executeUpdate();
                    }
                }
            } catch (SQLException e) {
                fail(e.getMessage());
            }
        });
        LOGGER.info("{}.testInsert for defaultExecuteBatch {}, took {} millis",
                getClass().getSimpleName(),
                defaultExecuteBatch,
                TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - startNanos));
    }

    protected int getPostCount() {
        return 1000;
    }

    protected int getPostCommentCount() {
        return 5;
    }
}
