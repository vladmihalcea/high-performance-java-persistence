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
        System.setProperty("com.zaxxer.hikari.timeoutMs.floor", "5");
        HikariConfig hikariConfig = new HikariConfig();
        hikariConfig.setDataSource(dataSourceProvider().dataSource());
        hikariConfig.setAutoCommit(false);
        hikariConfig.setMaximumPoolSize(1);
        hikariConfig.setConnectionTimeout(150);
        HikariDataSource poolingDataSource = new HikariDataSource(hikariConfig);

        int maxOverflowPoolSize = 5;
        int connectionAcquisitionThresholdMillis = 50;
        FlexyPoolDataSource<HikariDataSource> dataSource = new FlexyPoolDataSource<>(
            new FlexyPoolConfiguration.Builder<>(
                getClass().getSimpleName(),
                poolingDataSource,
                HikariCPPoolAdapter.FACTORY)
            .build(),
            new IncrementPoolOnTimeoutConnectionAcquisitionStrategy.Factory<>(
                maxOverflowPoolSize,
                connectionAcquisitionThresholdMillis
            )
        );

        return dataSource;
    }

    /*@Override
    protected DataSource actualDataSource() {
        HikariConfig hikariConfig = new HikariConfig();
        hikariConfig.setDataSource(dataSourceProvider().dataSource());
        hikariConfig.setAutoCommit(false);
        hikariConfig.setMaximumPoolSize(3);
        HikariDataSource poolingDataSource = new HikariDataSource(hikariConfig);

        FlexyPoolDataSource<HikariDataSource> dataSource = new FlexyPoolDataSource<>(
            new FlexyPoolConfiguration.Builder<>(
                getClass().getSimpleName(),
                poolingDataSource,
                HikariCPPoolAdapter.FACTORY)
            .build()
        );

        return dataSource;
    }*/
}
