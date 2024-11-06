package com.vladmihalcea.util.providers;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.SQLException;

/**
 * @author Vlad Mihalcea
 */
public abstract class AbstractContainerDataSourceProvider implements DataSourceProvider {

    @Override
    public DataSource dataSource() {
        DataSource dataSource = newDataSource();
        try (Connection connection = dataSource.getConnection()) {
            return dataSource;
        } catch (SQLException e) {
            throw new IllegalStateException(e);
        }
    }

    @Override
    public String url() {
        return defaultJdbcUrl();
    }

    protected abstract String defaultJdbcUrl();

    protected abstract DataSource newDataSource();
}
