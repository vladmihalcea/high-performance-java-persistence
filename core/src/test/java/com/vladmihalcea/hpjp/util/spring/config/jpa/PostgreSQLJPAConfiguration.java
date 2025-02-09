package com.vladmihalcea.hpjp.util.spring.config.jpa;

import com.vladmihalcea.hpjp.util.providers.Database;
import org.postgresql.ds.PGSimpleDataSource;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.PropertySource;

import javax.sql.DataSource;

/**
 * @author Vlad Mihalcea
 */
@Configuration
@PropertySource({"/META-INF/jdbc-postgresql.properties"})
public class PostgreSQLJPAConfiguration extends AbstractJPAConfiguration {

    @Value("${jdbc.dataSourceClassName}")
    private String dataSourceClassName;

    @Value("${jdbc.username}")
    private String jdbcUser;

    @Value("${jdbc.password}")
    private String jdbcPassword;

    @Value("${jdbc.database}")
    private String jdbcDatabase;

    @Value("${jdbc.host}")
    private String jdbcHost;

    @Value("${jdbc.port}")
    private String jdbcPort;

    public PostgreSQLJPAConfiguration() {
        super(Database.POSTGRESQL);
    }

    @Override
    public DataSource actualDataSource() {
        PGSimpleDataSource dataSource = new PGSimpleDataSource();
        dataSource.setDatabaseName(jdbcDatabase);
        dataSource.setUser(jdbcUser);
        dataSource.setPassword(jdbcPassword);
        dataSource.setServerName(jdbcHost);
        dataSource.setPortNumber(Integer.valueOf(jdbcPort));
        return dataSource;
    }
}
