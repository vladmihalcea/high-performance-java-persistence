package com.vladmihalcea.hpjp.spring.transaction.transfer.config;

import com.vladmihalcea.flexypool.FlexyPoolDataSource;
import com.vladmihalcea.flexypool.adaptor.HikariCPPoolAdapter;
import com.vladmihalcea.flexypool.config.FlexyPoolConfiguration;
import com.vladmihalcea.flexypool.strategy.IncrementPoolOnTimeoutConnectionAcquisitionStrategy;
import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;

import javax.sql.DataSource;

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

        FlexyPoolDataSource<HikariDataSource> flexyPoolDataSource = new FlexyPoolDataSource<>(
            new FlexyPoolConfiguration.Builder<>(
                getClass().getSimpleName(),
                hikariDataSource,
                HikariCPPoolAdapter.FACTORY)
            .build(),
            new IncrementPoolOnTimeoutConnectionAcquisitionStrategy.Factory<>(5, 1)
        );

        return flexyPoolDataSource;
    }
}
