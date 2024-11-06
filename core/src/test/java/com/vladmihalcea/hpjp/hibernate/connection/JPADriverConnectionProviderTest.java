package com.vladmihalcea.hpjp.hibernate.connection;

import com.vladmihalcea.hpjp.util.providers.DataSourceProvider;

import java.util.Properties;

public class JPADriverConnectionProviderTest extends DriverManagerConnectionProviderTest {

    protected void appendDriverProperties(Properties properties) {
        DataSourceProvider dataSourceProvider = dataSourceProvider();
        properties.put("jakarta.persistence.jdbc.driver", dataSourceProvider.driverClassName().getName());
        properties.put("jakarta.persistence.jdbc.url", dataSourceProvider.url());
        properties.put("jakarta.persistence.jdbc.user", dataSourceProvider.username());
        properties.put("jakarta.persistence.jdbc.password", dataSourceProvider.password());
    }
}
