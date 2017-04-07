package com.vladmihalcea.book.hpjp.hibernate.connection;

import org.hibernate.hikaricp.internal.HikariCPConnectionProvider;

import java.util.Properties;

import com.vladmihalcea.book.hpjp.util.providers.DataSourceProvider;

public class HikariCPConnectionProviderTest extends DriverConnectionProviderTest {

    @Override
    protected void appendDriverProperties(Properties properties) {
        DataSourceProvider dataSourceProvider = dataSourceProvider();
        properties.put("hibernate.connection.provider_class", HikariCPConnectionProvider.class.getName());
        properties.put("hibernate.hikari.maximumPoolSize", "5");
        properties.put("hibernate.hikari.dataSourceClassName", dataSourceProvider.dataSourceClassName().getName());
        properties.put("hibernate.hikari.dataSource.url", dataSourceProvider.url());
        properties.put("hibernate.hikari.dataSource.user", dataSourceProvider.username());
        properties.put("hibernate.hikari.dataSource.password", dataSourceProvider.password());
    }
}
