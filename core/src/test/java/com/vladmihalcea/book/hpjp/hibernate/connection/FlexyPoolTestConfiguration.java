package com.vladmihalcea.book.hpjp.hibernate.connection;

import com.vladmihalcea.book.hpjp.hibernate.connection.jta.FlexyPoolEntities;
import com.vladmihalcea.book.hpjp.util.spring.config.jpa.HikariCPPostgreSQLJPAConfiguration;
import com.vladmihalcea.book.hpjp.util.spring.config.jpa.PostgreSQLJPAConfiguration;
import com.vladmihalcea.flexypool.FlexyPoolDataSource;
import com.vladmihalcea.flexypool.adaptor.DataSourcePoolAdapter;
import com.vladmihalcea.flexypool.adaptor.HikariCPPoolAdapter;
import com.zaxxer.hikari.HikariDataSource;
import org.springframework.context.annotation.Configuration;

import javax.sql.DataSource;

@Configuration
public class FlexyPoolTestConfiguration extends HikariCPPostgreSQLJPAConfiguration {

    @Override
    protected Class configurationClass() {
        return FlexyPoolEntities.class;
    }

    @Override
    public DataSource actualDataSource() {
        final HikariDataSource dataSource = (HikariDataSource) super.actualDataSource();

        com.vladmihalcea.flexypool.config.Configuration<HikariDataSource> configuration =
            new com.vladmihalcea.flexypool.config.Configuration.Builder<>(
                "flexy-pool-test",
                dataSource,
                HikariCPPoolAdapter.FACTORY
            )
        .build();

        return new FlexyPoolDataSource<>(
            configuration
        );
    }
}
