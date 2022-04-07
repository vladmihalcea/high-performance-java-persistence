package com.vladmihalcea.book.hpjp.util.spring.config.jta;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.PropertySource;
import org.springframework.jdbc.datasource.DriverManagerDataSource;

import javax.sql.DataSource;

/**
 * @author Vlad Mihalcea
 */
@PropertySource({"/META-INF/jta-postgresql.properties"})
@Configuration
public abstract class PostgreSQLJTATransactionManagerConfiguration extends AbstractJTATransactionManagerConfiguration {

    @Value("${jdbc.dataSourceClassName}")
    protected String dataSourceClassName;

    @Value("${jdbc.username}")
    protected String jdbcUser;

    @Value("${jdbc.password}")
    protected String jdbcPassword;

    @Value("${jdbc.database}")
    protected String jdbcDatabase;

    @Value("${jdbc.host}")
    protected String jdbcHost;

    @Value("${jdbc.port}")
    protected String jdbcPort;

    @Value("${hibernate.dialect}")
    protected String hibernateDialect;

    public DataSource actualDataSource() {
        DriverManagerDataSource dataSource = new DriverManagerDataSource();
        dataSource.setDriverClassName(dataSourceClassName);
        dataSource.setUrl(
            String.format(
                "jdbc:postgresql://%s:%s/%s",
                jdbcHost,
                jdbcPort,
                jdbcDatabase
            )
        );
        dataSource.setUsername(jdbcUser);
        dataSource.setPassword(jdbcPassword);
        return dataSource;
    }
}
