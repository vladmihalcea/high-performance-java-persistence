package com.vladmihalcea.hpjp.util.providers;

import org.mariadb.jdbc.Driver;
import com.vladmihalcea.hpjp.util.providers.queries.MySQLQueries;
import com.vladmihalcea.hpjp.util.providers.queries.Queries;
import com.zaxxer.hikari.util.DriverDataSource;
import org.hibernate.dialect.MariaDBDialect;
import org.mariadb.jdbc.MariaDbDataSource;
import org.testcontainers.containers.JdbcDatabaseContainer;

import javax.sql.DataSource;
import java.sql.SQLException;
import java.util.Properties;

/**
 * @author Vlad Mihalcea
 */
public class MariaDBDataSourceProvider extends AbstractContainerDataSourceProvider {

    private boolean rewriteBatchedStatements = true;

    private boolean cachePrepStmts = false;

    private boolean useServerPrepStmts = false;

    private boolean useTimezone = false;

    private boolean useJDBCCompliantTimezoneShift = false;

    private boolean useLegacyDatetimeCode = true;

    public boolean isRewriteBatchedStatements() {
        return rewriteBatchedStatements;
    }

    public void setRewriteBatchedStatements(boolean rewriteBatchedStatements) {
        this.rewriteBatchedStatements = rewriteBatchedStatements;
    }

    public boolean isCachePrepStmts() {
        return cachePrepStmts;
    }

    public void setCachePrepStmts(boolean cachePrepStmts) {
        this.cachePrepStmts = cachePrepStmts;
    }

    public boolean isUseServerPrepStmts() {
        return useServerPrepStmts;
    }

    public void setUseServerPrepStmts(boolean useServerPrepStmts) {
        this.useServerPrepStmts = useServerPrepStmts;
    }

    public boolean isUseTimezone() {
        return useTimezone;
    }

    public void setUseTimezone(boolean useTimezone) {
        this.useTimezone = useTimezone;
    }

    public boolean isUseJDBCCompliantTimezoneShift() {
        return useJDBCCompliantTimezoneShift;
    }

    public void setUseJDBCCompliantTimezoneShift(boolean useJDBCCompliantTimezoneShift) {
        this.useJDBCCompliantTimezoneShift = useJDBCCompliantTimezoneShift;
    }

    public boolean isUseLegacyDatetimeCode() {
        return useLegacyDatetimeCode;
    }

    public void setUseLegacyDatetimeCode(boolean useLegacyDatetimeCode) {
        this.useLegacyDatetimeCode = useLegacyDatetimeCode;
    }

    @Override
    public String hibernateDialect() {
        return MariaDBDialect.class.getName();
    }

    @Override
    protected String defaultJdbcUrl() {
        return "jdbc:mariadb://localhost/high_performance_java_persistence " +
               "?rewriteBatchedStatements=" + rewriteBatchedStatements +
               "&cachePrepStmts=" + cachePrepStmts +
               "&useServerPrepStmts=" + useServerPrepStmts;
    }

    @Override
    protected DataSource newDataSource() {
        JdbcDatabaseContainer container = database().getContainer();
        if(container != null) {
            Properties properties = new Properties();
            properties.setProperty("rewriteBatchedStatements", String.valueOf(rewriteBatchedStatements));
            properties.setProperty("cachePrepStmts", String.valueOf(cachePrepStmts));
            properties.setProperty("useServerPrepStmts", String.valueOf(useServerPrepStmts));
            return new DriverDataSource(
                container.getJdbcUrl(),
                container.getDriverClassName(),
                properties,
                container.getUsername(),
                container.getPassword()
            );
        }
        MariaDbDataSource dataSource = new MariaDbDataSource();
        try {
            dataSource.setUrl(defaultJdbcUrl());
            dataSource.setUser(username());
            dataSource.setPassword(password());
        } catch (SQLException e) {
            throw new IllegalStateException(e);
        }
        return dataSource;
    }

    @Override
    public Class<? extends DataSource> dataSourceClassName() {
        return MariaDbDataSource.class;
    }

    @Override
    public Class driverClassName() {
        return Driver.class;
    }

    @Override
    public Properties dataSourceProperties() {
        Properties properties = new Properties();
        properties.setProperty("url", url());
        return properties;
    }

    @Override
    public String username() {
        return "mariadb";
    }

    @Override
    public String password() {
        return "admin";
    }

    @Override
    public Database database() {
        return Database.MARIADB;
    }

    @Override
    public String toString() {
        return "MariaDBDataSourceProvider{" +
                "rewriteBatchedStatements=" + rewriteBatchedStatements +
                ", cachePrepStmts=" + cachePrepStmts +
                ", useServerPrepStmts=" + useServerPrepStmts +
                ", useTimezone=" + useTimezone +
                ", useJDBCCompliantTimezoneShift=" + useJDBCCompliantTimezoneShift +
                ", useLegacyDatetimeCode=" + useLegacyDatetimeCode +
                '}';
    }

    @Override
    public Queries queries() {
        return MySQLQueries.INSTANCE;
    }
}
