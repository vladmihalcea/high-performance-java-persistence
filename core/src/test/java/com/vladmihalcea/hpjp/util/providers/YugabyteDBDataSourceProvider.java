package com.vladmihalcea.hpjp.util.providers;

import com.vladmihalcea.hpjp.util.providers.queries.PostgreSQLQueries;
import com.vladmihalcea.hpjp.util.providers.queries.Queries;
import com.zaxxer.hikari.util.DriverDataSource;
import org.hibernate.dialect.PostgreSQLDialect;
import org.postgresql.Driver;
import org.postgresql.ds.PGSimpleDataSource;
import org.testcontainers.containers.JdbcDatabaseContainer;

import javax.sql.DataSource;
import java.util.Properties;

/**
 * @author Vlad Mihalcea
 */
public class YugabyteDBDataSourceProvider extends AbstractContainerDataSourceProvider {

    @Override
    public String hibernateDialect() {
        return PostgreSQLDialect.class.getName();
    }

    @Override
    protected String defaultJdbcUrl() {
        return "jdbc:postgresql://127.0.0.1:5433/high_performance_java_persistence";
    }

    protected DataSource newDataSource() {
        JdbcDatabaseContainer container = database().getContainer();
        if(container != null) {
            return new DriverDataSource(
                container.getJdbcUrl(),
                container.getDriverClassName(),
                new Properties(),
                container.getUsername(),
                container.getPassword()
            );
        }
        PGSimpleDataSource dataSource = new PGSimpleDataSource();
        dataSource.setURL(url());
        dataSource.setUser(username());
        dataSource.setPassword(password());
        return dataSource;
    }

    @Override
    public Class<? extends DataSource> dataSourceClassName() {
        return PGSimpleDataSource.class;
    }

    @Override
    public Class driverClassName() {
        return Driver.class;
    }

    @Override
    public Properties dataSourceProperties() {
        Properties properties = new Properties();
        properties.setProperty("user", username());
        properties.setProperty("password", password());
        return properties;
    }

    @Override
    public String username() {
        return "yugabyte";
    }

    @Override
    public String password() {
        return "admin";
    }

    @Override
    public Database database() {
        return Database.YUGABYTEDB;
    }

    @Override
    public Queries queries() {
        return PostgreSQLQueries.INSTANCE;
    }
}
