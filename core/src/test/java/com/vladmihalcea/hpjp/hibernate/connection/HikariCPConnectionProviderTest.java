package com.vladmihalcea.hpjp.hibernate.connection;

import org.hibernate.engine.jdbc.connections.spi.ConnectionProvider;
import org.hibernate.hikaricp.internal.HikariCPConnectionProvider;

import java.util.Properties;

public class HikariCPConnectionProviderTest extends DriverManagerConnectionProviderTest {

    @Override
    protected void appendDriverProperties(Properties properties) {
        super.appendDriverProperties(properties);
        String maxPoolSize = String.valueOf(5);
        properties.put("hibernate.hikari.maximumPoolSize", maxPoolSize);
        properties.put("hibernate.hikari.minimumIdle", maxPoolSize);
    }

    @Override
    public Class<? extends ConnectionProvider> expectedConnectionProviderClass() {
        return HikariCPConnectionProvider.class;
    }
}
