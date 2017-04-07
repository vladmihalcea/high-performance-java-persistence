package com.vladmihalcea.book.hpjp.jdbc.transaction;

import com.vladmihalcea.book.hpjp.util.DataSourceProviderIntegrationTest;
import com.vladmihalcea.book.hpjp.util.providers.entity.BlogEntityProvider;
import com.vladmihalcea.book.hpjp.util.providers.DataSourceProvider;

import org.junit.Test;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;

/**
 * ConnectionReadyOnlyTransactionTest - Test to verify which driver supports read-only transactions
 *
 * @author Vlad Mihalcea
 */
public class ConnectionReadyOnlyTransactionTest extends DataSourceProviderIntegrationTest {
    public static final String INSERT_POST = "insert into post (title, version, id) values (?, ?, ?)";

    private BlogEntityProvider entityProvider = new BlogEntityProvider();

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
                LOGGER.info("Database {} allows writes in read-only connections", dataSourceProvider().database());
            } catch (SQLException e) {
                LOGGER.info("Database {} prevents writes in read-only connections", dataSourceProvider().database());
            }
        }, connection -> {
            try {
                setReadOnly(connection);
            } catch (SQLException e) {
                LOGGER.error("Database {} doesn't support read-only connections", dataSourceProvider().database());
            }
        });
    }

    protected void setReadOnly(Connection connection) throws SQLException {
        connection.setReadOnly(true);
    }
}
