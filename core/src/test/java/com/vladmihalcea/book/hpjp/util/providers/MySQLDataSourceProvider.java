package com.vladmihalcea.book.hpjp.util.providers;

import com.mysql.cj.jdbc.MysqlDataSource;
import com.vladmihalcea.book.hpjp.util.ReflectionUtils;
import com.vladmihalcea.book.hpjp.util.providers.queries.MySQLQueries;
import com.vladmihalcea.book.hpjp.util.providers.queries.Queries;
import org.hibernate.dialect.MySQL8Dialect;

import javax.sql.DataSource;
import java.sql.SQLException;
import java.util.Properties;

/**
 * @author Vlad Mihalcea
 */
public class MySQLDataSourceProvider implements DataSourceProvider {

    private boolean rewriteBatchedStatements = false;

    private boolean cachePrepStmts = false;

    private boolean useServerPrepStmts = false;

    private boolean useTimezone = false;

    private boolean useJDBCCompliantTimezoneShift = false;

    private boolean useLegacyDatetimeCode = true;

    private boolean useCursorFetch = false;

    private Integer prepStmtCacheSqlLimit = null;

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
        return "org.hibernate.dialect.MySQL8Dialect";
    }

    @Override
    public DataSource dataSource() {
        try {
            MysqlDataSource dataSource = new MysqlDataSource();

            String url = "jdbc:mysql://localhost/high_performance_java_persistence?useSSL=false";

            if(!MySQL8Dialect.class.isAssignableFrom(ReflectionUtils.getClass(hibernateDialect()))) {
                url += "&useTimezone=" + useTimezone +
                        "&useJDBCCompliantTimezoneShift=" + useJDBCCompliantTimezoneShift +
                        "&useLegacyDatetimeCode=" + useLegacyDatetimeCode;
            }

            dataSource.setURL(url);
            dataSource.setUser(username());
            dataSource.setPassword(password());

            dataSource.setRewriteBatchedStatements(rewriteBatchedStatements);
            dataSource.setUseCursorFetch(useCursorFetch);
            dataSource.setCachePrepStmts(cachePrepStmts);
            dataSource.setUseServerPrepStmts(useServerPrepStmts);
            if (prepStmtCacheSqlLimit != null) {
                dataSource.setPrepStmtCacheSqlLimit(prepStmtCacheSqlLimit);
            }

            return dataSource;
        } catch (SQLException e) {
            throw new IllegalArgumentException(e);
        }
    }

    @Override
    public Class<? extends DataSource> dataSourceClassName() {
        return MysqlDataSource.class;
    }

    @Override
    public Properties dataSourceProperties() {
        Properties properties = new Properties();
        properties.setProperty("url", url());
        return properties;
    }

    @Override
    public String url() {
        return "jdbc:mysql://localhost/high_performance_java_persistence?user=mysql&password=admin";
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
