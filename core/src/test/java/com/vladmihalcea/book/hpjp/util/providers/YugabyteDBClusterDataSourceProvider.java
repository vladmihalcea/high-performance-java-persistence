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
        return dataSource;
    }

    @Override
    public Class<? extends DataSource> dataSourceClassName() {
        return PGSimpleDataSource.class;
    }

    @Override
    public String url() {
        return "jdbc:yugabytedb://127.0.0.1:5433/high_performance_java_persistence?load-balance=true";
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
