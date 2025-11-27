package com.vladmihalcea.hpjp.jdbc.fetching;

import com.vladmihalcea.hpjp.util.AbstractTest;
import com.vladmihalcea.hpjp.util.providers.Database;
import com.vladmihalcea.hpjp.util.providers.entity.BlogEntityProvider;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.Parameter;
import org.junit.jupiter.params.ParameterizedClass;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.fail;

/**
 * ResultSetFetchSizeTest - Test result set fetch size
 *
 * @author Vlad Mihalcea
 */
@ParameterizedClass
@MethodSource("parameters")
public class ResultSetFetchSizeTest extends AbstractTest {

    public static final String INSERT_POST = "insert into post (title, version, id) values (?, ?, ?)";

    private BlogEntityProvider entityProvider = new BlogEntityProvider();

    @Parameter(0)
    private Database database;

    @Parameter(1)
    private Integer fetchSize;

    public static Stream<Arguments> parameters() {
        List<Arguments> arguments = new ArrayList<>();
        for (int i = 0; i < databases.length; i++) {
            for (int j = 0; j < fetchSizes.length; j++) {
                Integer fetchSize = fetchSizes[j];
                arguments.add(Arguments.of(databases[i], fetchSize));
            }
        }
        return arguments.stream();
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
    public void afterInit() {
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
