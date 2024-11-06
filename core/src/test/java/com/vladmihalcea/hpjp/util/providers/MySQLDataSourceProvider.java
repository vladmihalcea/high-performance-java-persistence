package com.vladmihalcea.hpjp.util.providers;

import com.mysql.cj.jdbc.Driver;
import com.mysql.cj.jdbc.MysqlDataSource;
import com.vladmihalcea.hpjp.util.providers.queries.MySQLQueries;
import com.vladmihalcea.hpjp.util.providers.queries.Queries;
import org.hibernate.dialect.MySQLDialect;

import javax.sql.DataSource;
import java.sql.SQLException;
import java.util.Properties;

/**
 * @author Vlad Mihalcea
 */
public class MySQLDataSourceProvider extends AbstractContainerDataSourceProvider {

    private Boolean rewriteBatchedStatements;

    private Boolean cachePrepStmts;

    private Boolean useServerPrepStmts;

    private Boolean useTimezone;

    private Boolean useJDBCCompliantTimezoneShift;

    private Boolean useLegacyDatetimeCode;

    private Boolean useCursorFetch;

    private Integer prepStmtCacheSqlLimit;

    public boolean isRewriteBatchedStatements() {
        return rewriteBatchedStatements;
    }

    public MySQLDataSourceProvider setRewriteBatchedStatements(boolean rewriteBatchedStatements) {
        this.rewriteBatchedStatements = rewriteBatchedStatements;
        return this;
    }

    public boolean isCachePrepStmts() {
        return cachePrepStmts;
    }

    public MySQLDataSourceProvider setCachePrepStmts(boolean cachePrepStmts) {
        this.cachePrepStmts = cachePrepStmts;
        return this;
    }

    public boolean isUseServerPrepStmts() {
        return useServerPrepStmts;
    }

    public MySQLDataSourceProvider setUseServerPrepStmts(boolean useServerPrepStmts) {
        this.useServerPrepStmts = useServerPrepStmts;
        return this;
    }

    public boolean isUseTimezone() {
        return useTimezone;
    }

    public MySQLDataSourceProvider setUseTimezone(boolean useTimezone) {
        this.useTimezone = useTimezone;
        return this;
    }

    public boolean isUseJDBCCompliantTimezoneShift() {
        return useJDBCCompliantTimezoneShift;
    }

    public MySQLDataSourceProvider setUseJDBCCompliantTimezoneShift(boolean useJDBCCompliantTimezoneShift) {
        this.useJDBCCompliantTimezoneShift = useJDBCCompliantTimezoneShift;
        return this;
    }

    public boolean isUseLegacyDatetimeCode() {
        return useLegacyDatetimeCode;
    }

    public MySQLDataSourceProvider setUseLegacyDatetimeCode(boolean useLegacyDatetimeCode) {
        this.useLegacyDatetimeCode = useLegacyDatetimeCode;
        return this;
    }

    public boolean isUseCursorFetch() {
        return useCursorFetch;
    }

    public MySQLDataSourceProvider setUseCursorFetch(boolean useCursorFetch) {
        this.useCursorFetch = useCursorFetch;
        return this;
    }

    public Integer getPrepStmtCacheSqlLimit() {
        return prepStmtCacheSqlLimit;
    }

    public MySQLDataSourceProvider setPrepStmtCacheSqlLimit(Integer prepStmtCacheSqlLimit) {
        this.prepStmtCacheSqlLimit = prepStmtCacheSqlLimit;
        return this;
    }

    @Override
    public String hibernateDialect() {
        return MySQLDialect.class.getName();
    }

    @Override
    protected String defaultJdbcUrl() {
        return "jdbc:mysql://localhost/high_performance_java_persistence?useSSL=false";
    }

    @Override
    protected DataSource newDataSource() {
        try {
            MysqlDataSource dataSource = new MysqlDataSource();
            dataSource.setURL(url());
            dataSource.setUser(username());
            dataSource.setPassword(password());

            if (rewriteBatchedStatements != null) {
                dataSource.setRewriteBatchedStatements(rewriteBatchedStatements);
            }
            if (useCursorFetch != null) {
                dataSource.setUseCursorFetch(useCursorFetch);
            }
            if (cachePrepStmts != null) {
                dataSource.setCachePrepStmts(cachePrepStmts);
            }
            if (useServerPrepStmts != null) {
                dataSource.setUseServerPrepStmts(useServerPrepStmts);
            }
            if (prepStmtCacheSqlLimit != null) {
                dataSource.setPrepStmtCacheSqlLimit(prepStmtCacheSqlLimit);
            }

            return dataSource;
        } catch (SQLException e) {
            throw new IllegalStateException("The DataSource could not be instantiated!");
        }
    }

    @Override
    public Class<? extends DataSource> dataSourceClassName() {
        return MysqlDataSource.class;
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
        return "mysql";
    }

    @Override
    public String password() {
        return "admin";
    }

    @Override
    public Database database() {
        return Database.MYSQL;
    }

    @Override
    public String toString() {
        return "MySQLDataSourceProvider{" +
               "cachePrepStmts=" + cachePrepStmts +
               ", useServerPrepStmts=" + useServerPrepStmts +
               ", rewriteBatchedStatements=" + rewriteBatchedStatements +
               '}';
    }

    @Override
    public Queries queries() {
        return MySQLQueries.INSTANCE;
    }
}
