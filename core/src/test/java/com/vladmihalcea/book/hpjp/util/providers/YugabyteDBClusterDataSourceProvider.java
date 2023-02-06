package com.vladmihalcea.book.hpjp.util.providers;

import com.vladmihalcea.book.hpjp.util.providers.queries.PostgreSQLQueries;
import com.vladmihalcea.book.hpjp.util.providers.queries.Queries;
import com.yugabyte.ysql.YBClusterAwareDataSource;
import org.hibernate.dialect.PostgreSQLDialect;
import org.postgresql.ds.PGSimpleDataSource;

import javax.sql.DataSource;
import java.util.Properties;

/**
 * @author Vlad Mihalcea
 */
public class YugabyteDBClusterDataSourceProvider extends YugabyteDBDataSourceProvider {

    public static final DataSourceProvider INSTANCE = new YugabyteDBDataSourceProvider();

    private String host = "127.0.0.1";

    private int port = 5433;

    private String database = "high_performance_java_persistence";

    @Override
    public String hibernateDialect() {
        return PostgreSQLDialect.class.getName();
    }

    @Override
    public DataSource dataSource() {
        YBClusterAwareDataSource dataSource = new YBClusterAwareDataSource();
        dataSource.setURL(url());
        dataSource.setUser(username());
        dataSource.setPassword(password());
        dataSource.setLoadBalanceHosts(true);
        dataSource.setConnectTimeout(10);
        dataSource.setSocketTimeout(10);
        return dataSource;
    }

    @Override
    public Class<? extends DataSource> dataSourceClassName() {
        return YBClusterAwareDataSource.class;
    }

    @Override
    public String url() {
        return String.format(
            "jdbc:yugabytedb://%s:%d/%s?load-balance=true",
            host,
            port,
            database
        );
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
