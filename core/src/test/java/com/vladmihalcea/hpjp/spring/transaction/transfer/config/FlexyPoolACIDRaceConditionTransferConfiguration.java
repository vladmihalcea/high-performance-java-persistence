package com.vladmihalcea.hpjp.spring.transaction.transfer.config;

import com.vladmihalcea.flexypool.FlexyPoolDataSource;
import com.vladmihalcea.flexypool.adaptor.HikariCPPoolAdapter;
import com.vladmihalcea.flexypool.config.Configuration;
import com.vladmihalcea.flexypool.strategy.IncrementPoolOnTimeoutConnectionAcquiringStrategy;
import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import org.springframework.context.annotation.Bean;

import javax.sql.DataSource;
import java.util.concurrent.TimeUnit;

/**
 *
 * @author Vlad Mihalcea
 */
public class FlexyPoolACIDRaceConditionTransferConfiguration extends ACIDRaceConditionTransferConfiguration {

    @Override
    protected DataSource actualDataSource() {
        HikariConfig hikariConfig = new HikariConfig();
        hikariConfig.setMaximumPoolSize(1);
        hikariConfig.setAutoCommit(false);
        hikariConfig.setDataSource(dataSourceProvider().dataSource());
        HikariDataSource hikariDataSource = new HikariDataSource(hikariConfig);

        Configuration<HikariDataSource> flexyPoolConfiguration = new Configuration.Builder<>(
            getClass().getSimpleName(),
            hikariDataSource,
            HikariCPPoolAdapter.FACTORY
        )
        .build();

        FlexyPoolDataSource<HikariDataSource> flexyPoolDataSource = new FlexyPoolDataSource<>(
            flexyPoolConfiguration,
            new IncrementPoolOnTimeoutConnectionAcquiringStrategy.Factory<>(5, 15)
        );

        return flexyPoolDataSource;
    }
}
