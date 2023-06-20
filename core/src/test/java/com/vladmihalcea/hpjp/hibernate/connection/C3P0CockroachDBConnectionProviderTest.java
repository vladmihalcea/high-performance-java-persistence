package com.vladmihalcea.hpjp.hibernate.connection;

import java.util.Properties;

import com.vladmihalcea.hpjp.util.providers.CockroachDBDataSourceProvider;
import com.vladmihalcea.hpjp.util.providers.DataSourceProvider;
import org.junit.Ignore;
import org.junit.Test;

public class C3P0CockroachDBConnectionProviderTest extends JPADriverConnectionProviderTest {

    protected DataSourceProvider dataSourceProvider() {
        return new CockroachDBDataSourceProvider();
    }

    @Override
    protected void appendDriverProperties(Properties properties) {
        super.appendDriverProperties(properties);
        properties.put("hibernate.c3p0.min_size", 1);
        properties.put("hibernate.c3p0.max_size", 5);
    }

    @Test
    @Override
    @Ignore
    public void testConnection() {
        super.testConnection();
    }
}
