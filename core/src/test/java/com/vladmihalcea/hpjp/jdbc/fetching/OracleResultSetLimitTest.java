package com.vladmihalcea.hpjp.jdbc.fetching;

import com.vladmihalcea.hpjp.util.DatabaseProviderIntegrationTest;
import com.vladmihalcea.hpjp.util.providers.Database;
import com.vladmihalcea.hpjp.util.providers.entity.BlogEntityProvider;
import org.assertj.core.util.Arrays;
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
 * OracleResultSetLimitTest - Test limiting result set vs fetching and discarding rows
 *
 * @author Vlad Mihalcea
 */
public class OracleResultSetLimitTest extends DatabaseProviderIntegrationTest {
    public static final String INSERT_POST = "insert into post (title, version, id) values (?, ?, ?)";

    public static final String SELECT_POST =
        "SELECT p.id AS p_id  " +
        "FROM post p ";

    private BlogEntityProvider entityProvider = new BlogEntityProvider();

    public OracleResultSetLimitTest(Database database) {
        super(database);
    }

    @Parameterized.Parameters
    public static Collection<Database[]> databases() {
        List<Database[]> databases = new ArrayList<>();
        databases.add(Arrays.array(Database.ORACLE));
        return databases;
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
    public void testLimit() {
        long startNanos = System.nanoTime();
        doInJDBC(connection -> {
            try (PreparedStatement statement = connection.prepareStatement(SELECT_POST)
            ) {
                statement.setMaxRows(getMaxRows());
                assertEquals(getMaxRows(), processResultSet(statement));
            } catch (SQLException e) {
                fail(e.getMessage());
            }

        });
        LOGGER.info("{} Result Set with limit took {} millis",
                dataSourceProvider().database(),
                TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - startNanos));
    }

    protected int processResultSet(PreparedStatement statement) throws SQLException {
        statement.execute();
        int count = 0;
        ResultSet resultSet = statement.getResultSet();
        while (resultSet.next()) {
            resultSet.getLong(1);
            count++;
        }
        return count;
    }

    protected int getPostCount() {
        return 100;
    }

    protected int getPostCommentCount() {
        return 10;
    }

    protected int getMaxRows() {
        return 5;
    }


    @Override
    protected boolean proxyDataSource() {
        return false;
    }
}
