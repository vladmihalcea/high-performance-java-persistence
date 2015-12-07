package com.vladmihalcea.book.hpjp.util.spring.config;

import bitronix.tm.resource.jdbc.PoolingDataSource;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.*;

import javax.sql.DataSource;
import java.util.Properties;

/**
 * <code>JPAConfig</code> - JPAConfig
 *
 * @author Vlad Mihalcea
 */
@PropertySource({"/META-INF/jdbc-hsqldb.properties"})
@Configuration
public abstract class HsqldbJtaTransactionManagerConfiguration extends AbstractJtaTransactionManagerConfiguration{

    @Value("${jdbc.dataSourceClassName}")
    private String dataSourceClassName;

    @Value("${jdbc.username}")
    private String jdbcUser;

    @Value("${jdbc.password}")
    private String jdbcPassword;

    @Value("${jdbc.url}")
    private String jdbcUrl;
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
        poolingDataSource.getDriverProperties().put("url", jdbcUrl);
        return poolingDataSource;
    }
}
