package com.vladmihalcea.hpjp.hibernate.connection;

import org.hibernate.c3p0.internal.C3P0ConnectionProvider;
import org.hibernate.engine.jdbc.connections.spi.ConnectionProvider;

import java.util.Properties;

public class C3P0JPAConnectionProviderTest extends DriverManagerConnectionProviderTest {

    @Override
    protected void appendDriverProperties(Properties properties) {
        super.appendDriverProperties(properties);
        properties.put("hibernate.c3p0.min_size", 1);
        properties.put("hibernate.c3p0.max_size", 5);
    }

    @Override
    public Class<? extends ConnectionProvider> expectedConnectionProviderClass() {
        return C3P0ConnectionProvider.class;
    }
}
