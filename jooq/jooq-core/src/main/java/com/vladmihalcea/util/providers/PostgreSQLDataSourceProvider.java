package com.vladmihalcea.util.providers;

import org.hibernate.dialect.PostgreSQLDialect;
import org.postgresql.Driver;
import org.postgresql.ds.PGSimpleDataSource;

import javax.sql.DataSource;
import java.util.Properties;

/**
 * @author Vlad Mihalcea
 */
public class PostgreSQLDataSourceProvider extends AbstractContainerDataSourceProvider {

    private Boolean reWriteBatchedInserts;

    public boolean getReWriteBatchedInserts() {
        return reWriteBatchedInserts;
    }

    public PostgreSQLDataSourceProvider setReWriteBatchedInserts(boolean reWriteBatchedInserts) {
        this.reWriteBatchedInserts = reWriteBatchedInserts;
        return this;
    }

    @Override
    public String hibernateDialect() {
        return PostgreSQLDialect.class.getName();
    }

    @Override
    protected String defaultJdbcUrl() {
        return "jdbc:postgresql://localhost/high_performance_java_persistence";
    }

    protected DataSource newDataSource() {
        PGSimpleDataSource dataSource = new PGSimpleDataSource();
        dataSource.setURL(url());
        dataSource.setUser(username());
        dataSource.setPassword(password());
        if (reWriteBatchedInserts != null) {
            dataSource.setReWriteBatchedInserts(reWriteBatchedInserts);
        }

        return dataSource;
    }

    @Override
    public Class<? extends DataSource> dataSourceClassName() {
        return PGSimpleDataSource.class;
    }

    @Override
    public Properties dataSourceProperties() {
        Properties properties = new Properties();
        properties.setProperty("databaseName", "high_performance_java_persistence");
        properties.setProperty("serverName", "localhost");
        properties.setProperty("user", username());
        properties.setProperty("password", password());
        if (reWriteBatchedInserts != null) {
            properties.setProperty("reWriteBatchedInserts", String.valueOf(reWriteBatchedInserts));
        }
        return properties;
    }

    @Override
    public String username() {
        return "postgres";
    }

    @Override
    public String password() {
        return "admin";
    }

    @Override
    public Database database() {
        return Database.POSTGRESQL;
    }
}
