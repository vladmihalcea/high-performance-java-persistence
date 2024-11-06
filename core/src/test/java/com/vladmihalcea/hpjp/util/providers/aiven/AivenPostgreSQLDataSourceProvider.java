package com.vladmihalcea.hpjp.util.providers.aiven;

import com.vladmihalcea.hpjp.util.providers.AbstractContainerDataSourceProvider;
import com.vladmihalcea.hpjp.util.providers.Database;
import com.vladmihalcea.hpjp.util.providers.queries.PostgreSQLQueries;
import com.vladmihalcea.hpjp.util.providers.queries.Queries;
import org.hibernate.dialect.PostgreSQLDialect;
import org.postgresql.Driver;
import org.postgresql.ds.PGSimpleDataSource;

import javax.sql.DataSource;
import java.util.Properties;

/**
 * @author Vlad Mihalcea
 */
public class AivenPostgreSQLDataSourceProvider extends AbstractContainerDataSourceProvider {

    private interface Aiven {
        String URL = "AIVEN_URL";
        String USER = "AIVEN_USER";
        String PASS = "AIVEN_PASS";
    }

    private Boolean reWriteBatchedInserts;

    public boolean getReWriteBatchedInserts() {
        return reWriteBatchedInserts;
    }

    public AivenPostgreSQLDataSourceProvider setReWriteBatchedInserts(boolean reWriteBatchedInserts) {
        this.reWriteBatchedInserts = reWriteBatchedInserts;
        return this;
    }

    @Override
    public String hibernateDialect() {
        return PostgreSQLDialect.class.getName();
    }

    @Override
    protected String defaultJdbcUrl() {
        return String.format(
            "jdbc:postgresql://%s?ssl=require",
            System.getenv().get(Aiven.URL)
        );
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
    public Class driverClassName() {
        return Driver.class;
    }

    @Override
    public Properties dataSourceProperties() {
        return null;
    }

    @Override
    public String username() {
        return System.getenv().get(Aiven.USER);
    }

    @Override
    public String password() {
        return System.getenv().get(Aiven.PASS);
    }

    @Override
    public Database database() {
        return Database.POSTGRESQL;
    }

    @Override
    public Queries queries() {
        return PostgreSQLQueries.INSTANCE;
    }
}
