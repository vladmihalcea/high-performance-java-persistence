package com.vladmihalcea.hpjp.jdbc.fetching;

import com.vladmihalcea.hpjp.util.DatabaseProviderIntegrationTest;
import com.vladmihalcea.hpjp.util.providers.*;
import com.vladmihalcea.hpjp.util.providers.entity.BlogEntityProvider;

import org.junit.Test;
import org.junit.runners.Parameterized;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.TimeUnit;

import static org.junit.Assert.fail;

/**
 * ResultSetFetchSizeTest - Test result set fetch size
 *
 * @author Vlad Mihalcea
 */
public class ResultSetFetchSizeTest extends DatabaseProviderIntegrationTest {

    public static final String INSERT_POST = "insert into post (title, version, id) values (?, ?, ?)";

    private BlogEntityProvider entityProvider = new BlogEntityProvider();

    private final Integer fetchSize;

    public ResultSetFetchSizeTest(Database database, Integer fetchSize) {
        super(database);
        this.fetchSize = fetchSize;
    }

    @Parameterized.Parameters
    public static Collection<Object[]> parameters() {
        List<Object[]> providers = new ArrayList<>();
        for (int i = 0; i < databases.length; i++) {
            for (int j = 0; j < fetchSizes.length; j++) {
                Integer fetchSize = fetchSizes[j];
                providers.add(new Object[] {databases[i], fetchSize});
            }
        }
        return providers;
    }

    private static Integer[] fetchSizes = new Integer[] {
            //null, 1, 10, 100, 1000, 10000
            1, 10, 100, 1000, 10000
    };

    private static Database[] databases = new Database[]{
        Database.ORACLE,
        Database.SQLSERVER,
        Database.POSTGRESQL,
        Database.MYSQL,
    };

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
    public void testFetchSize() {
        long startNanos = System.nanoTime();
        doInJDBC(connection -> {
            try (PreparedStatement statement = connection.prepareStatement(
                    "select * from post"
            )) {
                if (fetchSize != null) {
                    statement.setFetchSize(fetchSize);
                }
                statement.execute();
                ResultSet resultSet = statement.getResultSet();
                while (resultSet.next()) {
                    resultSet.getLong(1);
                }
            } catch (SQLException e) {
                fail(e.getMessage());
            }

        });
        LOGGER.info("{} fetch size {} took {} millis",
                dataSourceProvider().database(),
                fetchSize != null ? fetchSize : "N/A",
                TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - startNanos));
    }

    protected int getPostCount() {
        return 10000;
    }

    @Override
    protected boolean proxyDataSource() {
        return false;
    }
}
