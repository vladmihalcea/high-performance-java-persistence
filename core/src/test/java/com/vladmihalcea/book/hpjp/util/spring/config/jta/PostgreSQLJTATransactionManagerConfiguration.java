package com.vladmihalcea.book.hpjp.util.spring.config.jta;

import bitronix.tm.resource.jdbc.PoolingDataSource;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.PropertySource;

import javax.sql.DataSource;
import java.util.Properties;

/**
 * @author Vlad Mihalcea
 */
@PropertySource({"/META-INF/jta-postgresql.properties"})
@Configuration
public abstract class PostgreSQLJTATransactionManagerConfiguration extends AbstractJTATransactionManagerConfiguration {

    @Value("${jdbc.dataSourceClassName}")
    private String dataSourceClassName;

    @Value("${btm.config.journal:disk}")
    private String btmJournal;

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

    @Value("${hibernate.dialect}")
    private String hibernateDialect;

    public DataSource actualDataSource() {
        PoolingDataSource poolingDataSource = new PoolingDataSource();
        poolingDataSource.setClassName(dataSourceClassName);
        poolingDataSource.setUniqueName(getClass().getName());
        poolingDataSource.setMinPoolSize(0);
        poolingDataSource.setMaxPoolSize(5);
        poolingDataSource.setAllowLocalTransactions(true);
        poolingDataSource.setDriverProperties(new Properties());
        poolingDataSource.getDriverProperties().put("user", jdbcUser);
        poolingDataSource.getDriverProperties().put("password", jdbcPassword);
        poolingDataSource.getDriverProperties().put("databaseName", jdbcDatabase);
        poolingDataSource.getDriverProperties().put("serverName", jdbcHost);
        poolingDataSource.getDriverProperties().put("portNumber", jdbcPort);
        return poolingDataSource;
    }
}
