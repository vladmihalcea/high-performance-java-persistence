package com.vladmihalcea.hpjp.util.providers;

import org.testcontainers.containers.JdbcDatabaseContainer;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.SQLException;

/**
 * To test the Docker integration.
 *
 * Check the available PostgreSQL service:
 *
 * sc queryex type=service state=all | find /i "SERVICE_NAME: postgres"
 *
 * net stop postgresql-x64-15
 *
 * @author Vlad Mihalcea
 */
public abstract class AbstractContainerDataSourceProvider implements DataSourceProvider {

    @Override
    public DataSource dataSource() {
        DataSource dataSource = newDataSource();
        try(Connection connection = dataSource.getConnection()) {
            return dataSource;
        } catch (SQLException e) {
            Database database = database();
            if(database.getContainer() == null) {
                database.initContainer(username(), password());
            }
            return newDataSource();
        }
    }

    @Override
    public String url() {
        JdbcDatabaseContainer container = database().getContainer();
        return container != null ?
            container.getJdbcUrl() :
            defaultJdbcUrl();
    }

    protected abstract String defaultJdbcUrl();

    protected abstract DataSource newDataSource();
}
