package com.vladmihalcea.book.hpjp.jdbc.fetching;

import com.vladmihalcea.book.hpjp.util.DataSourceProviderIntegrationTest;
import com.vladmihalcea.book.hpjp.util.providers.DataSourceProvider;
import com.vladmihalcea.book.hpjp.util.providers.PostgreSQLDataSourceProvider;
import com.vladmihalcea.book.hpjp.util.providers.SQLServerDataSourceProvider;
import com.vladmihalcea.book.hpjp.util.providers.entity.BlogEntityProvider;
import org.junit.Test;
import org.junit.runners.Parameterized;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.TimeUnit;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

/**
 * SQLStandardResultSetLimitTest - Test limiting result set vs fetching and discarding rows
 *
 * @author Vlad Mihalcea
 */
public class SQLStandardResultSetLimitTest extends DataSourceProviderIntegrationTest {
    public static final String INSERT_POST = "insert into post (title, version, id) values (?, ?, ?)";

    public static final String INSERT_POST_COMMENT = "insert into post_comment (post_id, review, version, id) values (?, ?, ?, ?)";

    public static final String SELECT_POST_COMMENT =
        "SELECT pc.id AS pc_id, p.id AS p_id  " +
        "FROM post_comment pc " +
        "INNER JOIN post p ON p.id = pc.post_id " +
        "ORDER BY pc_id, p_id " +
        "OFFSET ? ROWS " +
        "FETCH FIRST (?) ROWS ONLY ";

    public static final String SELECT_POST_COMMENT_WITH_NO_FIX =
            "SELECT pc.id AS pc_id, p.id AS p_id  " +
                    "FROM post_comment pc " +
                    "INNER JOIN post p ON p.id = pc.post_id " +
                    "ORDER BY pc_id, p_id " +
                    "OFFSET ? ROWS " +
                    "FETCH FIRST ? ROWS ONLY ";

    private BlogEntityProvider entityProvider = new BlogEntityProvider();

    public SQLStandardResultSetLimitTest(DataSourceProvider dataSourceProvider) {
        super(dataSourceProvider);
    }

    @Parameterized.Parameters
    public static Collection<DataSourceProvider[]> rdbmsDataSourceProvider() {
        List<DataSourceProvider[]> providers = new ArrayList<>();
        providers.add(new DataSourceProvider[]{new SQLServerDataSourceProvider()});
        providers.add(new DataSourceProvider[]{new PostgreSQLDataSourceProvider()});
        return providers;
    }

    @Override
    protected Class<?>[] entities() {
        return entityProvider.entities();
    }

    @Override
    public void init() {
        super.init();
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
                    if (i > 0 && i % 100 == 0) {
                        postStatement.executeBatch();
                    }
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
    }

    @Test
    public void testLimit() {
        long startNanos = System.nanoTime();
        doInJDBC(connection -> {
            try (PreparedStatement statement = connection.prepareStatement(SELECT_POST_COMMENT);
                 PreparedStatement noFixStatement = connection.prepareStatement(SELECT_POST_COMMENT_WITH_NO_FIX);
            ) {
                pocessResultSet(statement);
                try {
                    pocessResultSet(noFixStatement);
                } catch (SQLException e) {
                    LOGGER.error("Possible bug:", e);
                }
            } catch (SQLException e) {
                fail(e.getMessage());
            }

        });
        LOGGER.info("{} Result Set with limit took {} millis",
                dataSourceProvider().database(),
                TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - startNanos));
    }

    protected void pocessResultSet(PreparedStatement statement) throws SQLException {
        statement.setInt(1, 0);
        statement.setInt(2, getMaxRows());
        statement.execute();
        int count = 0;
        ResultSet resultSet = statement.getResultSet();
        while (resultSet.next()) {
            resultSet.getLong(1);
            count++;
        }
        assertEquals(getMaxRows(), count);
    }

    protected int getPostCount() {
        return 100;
    }

    protected int getPostCommentCount() {
        return 10;
    }

    protected int getMaxRows() {
        return 100;
    }


    @Override
    protected boolean proxyDataSource() {
        return false;
    }
}
