package com.vladmihalcea.hpjp.hibernate.connection;

import com.vladmihalcea.hpjp.util.providers.DataSourceProvider;

import java.util.Properties;

public class DriverManagerConnectionProviderJakartaTest extends DriverManagerConnectionProviderTest {

    protected void appendDriverProperties(Properties properties) {
        DataSourceProvider dataSourceProvider = dataSourceProvider();

        String url = dataSourceProvider.url();
        String username = dataSourceProvider.username();
        String password = dataSourceProvider.password();

        properties.put(
            "jakarta.persistence.jdbc.driver",
            dataSourceProvider.driverClassName().getName()
        );
        properties.put("jakarta.persistence.jdbc.url", url);
        properties.put("jakarta.persistence.jdbc.user", username);
        properties.put("jakarta.persistence.jdbc.password", password);
    }
}
