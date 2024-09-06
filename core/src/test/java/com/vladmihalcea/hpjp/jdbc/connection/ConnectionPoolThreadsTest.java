package com.vladmihalcea.hpjp.jdbc.connection;

import com.vladmihalcea.hpjp.util.AbstractTest;
import com.vladmihalcea.hpjp.util.providers.Database;
import com.zaxxer.hikari.HikariConfig;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.SQLException;

import static org.junit.Assert.assertTrue;

/**
 * @author Vlad Mihalcea
 */
public class ConnectionPoolThreadsTest extends AbstractTest {

    private final Logger LOGGER = LoggerFactory.getLogger(getClass());

    @Override
    protected Class<?>[] entities() {
        return new Class[]{};
    }

    @Test
    public void test() {
        try(Connection connection = dataSource().getConnection()) {
            executeSync(() -> {
                try(Connection _connection = dataSource().getConnection()) {
                    assertTrue(connection != _connection);
                } catch (SQLException e) {
                    throw new RuntimeException(e);
                }
            });
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    protected Database database() {
        return Database.POSTGRESQL;
    }

    @Override
    protected HikariConfig hikariConfig(DataSource dataSource) {
        HikariConfig config = super.hikariConfig(dataSource);
        config.setMinimumIdle(0);
        return config;
    }

    protected boolean connectionPooling() {
        return true;
    }
}
