package com.vladmihalcea.hpjp.hibernate.connection;

import com.vladmihalcea.flexypool.FlexyPoolDataSource;
import com.vladmihalcea.flexypool.adaptor.HikariCPPoolAdapter;
import com.vladmihalcea.flexypool.config.FlexyPoolConfiguration;
import com.vladmihalcea.hpjp.hibernate.connection.jta.FlexyPoolEntities;
import com.vladmihalcea.hpjp.util.spring.config.jpa.HikariCPJPAConfiguration;
import com.zaxxer.hikari.HikariDataSource;
import org.springframework.context.annotation.Configuration;

import javax.sql.DataSource;

@Configuration
public class FlexyPoolTestConfiguration extends HikariCPJPAConfiguration {

    @Override
    protected Class configurationClass() {
        return FlexyPoolEntities.class;
    }

    @Override
    public DataSource actualDataSource() {
        final HikariDataSource dataSource = (HikariDataSource) super.actualDataSource();

        FlexyPoolConfiguration<HikariDataSource> configuration =
            new FlexyPoolConfiguration.Builder<>(
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
