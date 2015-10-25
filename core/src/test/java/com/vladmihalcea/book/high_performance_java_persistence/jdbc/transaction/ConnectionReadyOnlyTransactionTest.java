package com.vladmihalcea.book.high_performance_java_persistence.jdbc.transaction;

import com.vladmihalcea.book.high_performance_java_persistence.util.DataSourceProviderIntegrationTest;
import com.vladmihalcea.book.high_performance_java_persistence.util.providers.BatchEntityProvider;
import org.hibernate.dialect.pagination.LimitHandler;
import org.hibernate.engine.spi.RowSelection;
import org.hibernate.internal.SessionFactoryImpl;
import org.junit.Test;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.concurrent.TimeUnit;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

/**
 * ConnectionReadyOnlyTransactionTest - Test to verify which driver supports read-only transactions
 *
 * @author Vlad Mihalcea
 */
public class ConnectionReadyOnlyTransactionTest extends DataSourceProviderIntegrationTest {
    public static final String INSERT_POST = "insert into post (title, version, id) values (?, ?, ?)";

    private BatchEntityProvider entityProvider = new BatchEntityProvider();

    public ConnectionReadyOnlyTransactionTest(DataSourceProvider dataSourceProvider) {
        super(dataSourceProvider);
    }

    @Override
    protected Class<?>[] entities() {
        return entityProvider.entities();
    }

    @Test
    public void testReadOnlyDisallowsWrite() {
        transact(connection -> {
            try (
                    PreparedStatement postStatement = connection.prepareStatement(INSERT_POST)
            ) {
                int index = 0;
                postStatement.setString(++index, String.format("Post no. %1$d", 1));
                postStatement.setInt(++index, 0);
                postStatement.setLong(++index, 1);
                postStatement.executeUpdate();
                LOGGER.info("Database {} allows writes in read-only connections", getDataSourceProvider().database());
            } catch (SQLException e) {
                LOGGER.info("Database {} prevents writes in read-only connections", getDataSourceProvider().database());
            }
        }, connection -> {
            try {
                setReadOnly(connection);
            } catch (SQLException e) {
                LOGGER.error("Database {} doesn't support read-only connections", getDataSourceProvider().database());
            }
        });
    }

    protected void setReadOnly(Connection connection) throws SQLException {
        connection.setReadOnly(true);
    }
}
