package com.vladmihalcea.hpjp.util.spring.config.jta;

import com.atomikos.jdbc.AtomikosDataSourceBean;
import org.postgresql.xa.PGXADataSource;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.PropertySource;
import org.springframework.jdbc.datasource.DriverManagerDataSource;

import javax.sql.DataSource;
import java.util.Properties;

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

    @Bean(initMethod = "init", destroyMethod = "close")
    public AtomikosDataSourceBean actualDataSource() {
        AtomikosDataSourceBean dataSource = new AtomikosDataSourceBean();
        dataSource.setUniqueResourceName("PostgreSQL");
        PGXADataSource xaDataSource = new PGXADataSource();
        xaDataSource.setUser(jdbcUser);
        xaDataSource.setPassword(jdbcPassword);
        dataSource.setXaDataSource(xaDataSource);
        dataSource.setPoolSize(5);
        return dataSource;
    }
}
