package com.vladmihalcea.book.hpjp.util.spring.config.jpa;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.PropertySource;

import javax.sql.DataSource;
import java.util.Properties;

/**
 * @author Vlad Mihalcea
 */
@Configuration
@PropertySource({"/META-INF/jdbc-postgresql.properties"})
public class HikariCPPostgreSQLJPAConfiguration extends AbstractJPAConfiguration {

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

    @Override
    public DataSource actualDataSource() {
        Properties dataSourceProperties = new Properties();
        dataSourceProperties.setProperty("user", jdbcUser);
        dataSourceProperties.setProperty("password", jdbcPassword);
        dataSourceProperties.setProperty("databaseName", jdbcDatabase);
        dataSourceProperties.setProperty("serverName", jdbcHost);
        dataSourceProperties.setProperty("portNumber", jdbcPort);

        HikariConfig hikariConfig = new HikariConfig();
        hikariConfig.setDataSourceClassName(dataSourceClassName);
        hikariConfig.setDataSourceProperties(dataSourceProperties);
        hikariConfig.setMinimumPoolSize(1);
        hikariConfig.setMaximumPoolSize(3);

        return new HikariDataSource(hikariConfig);
    }

    @Override
    protected String databaseType() {
        return "postgresql";
    }
}
