package com.vladmihalcea.hpjp.hibernate.connection;

import com.vladmihalcea.hpjp.util.providers.CockroachDBDataSourceProvider;
import com.vladmihalcea.hpjp.util.providers.DataSourceProvider;
import org.junit.Test;

import java.util.Properties;

public class C3P0CockroachDBConnectionProviderTest extends JPADriverConnectionProviderTest {

    protected DataSourceProvider dataSourceProvider() {
        return new CockroachDBDataSourceProvider();
    }

    @Override
    public void init() {
        if(!ENABLE_LONG_RUNNING_TESTS) {
            return;
        }
        super.init();
    }

    @Override
    protected void appendDriverProperties(Properties properties) {
        super.appendDriverProperties(properties);
        properties.put("hibernate.c3p0.min_size", 1);
        properties.put("hibernate.c3p0.max_size", 5);
    }

    @Test
    @Override
    public void testConnection() {
        if(!ENABLE_LONG_RUNNING_TESTS) {
            return;
        }
        super.testConnection();
    }
}
