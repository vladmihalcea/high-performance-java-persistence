package com.vladmihalcea.book.hpjp.util.spring.config.jpa;

import com.vladmihalcea.book.hpjp.util.providers.DataSourceProvider;
import com.vladmihalcea.book.hpjp.util.providers.Database;
import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
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

    protected HikariCPPostgreSQLJPAConfiguration() {
        super(Database.YUGABYTEDB_CLUSTER);
    }

    @Bean
    public DataSourceProvider dataSourceProvider() {
        return database().dataSourceProvider();
    }

    @Bean(destroyMethod = "close")
    public DataSource actualDataSource() {
        HikariConfig hikariConfig = new HikariConfig();
        hikariConfig.setMaximumPoolSize(64);
        hikariConfig.setAutoCommit(false);
        hikariConfig.setDataSource(dataSourceProvider().dataSource());
        return new HikariDataSource(hikariConfig);
    }

    @Override
    protected String databaseType() {
        return "postgresql";
    }
}
