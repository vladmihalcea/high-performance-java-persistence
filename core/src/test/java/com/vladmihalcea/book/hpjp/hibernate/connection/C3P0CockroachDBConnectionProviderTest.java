package com.vladmihalcea.book.hpjp.hibernate.connection;

import java.util.Properties;

import com.vladmihalcea.book.hpjp.util.providers.CockroachDBDataSourceProvider;
import com.vladmihalcea.book.hpjp.util.providers.DataSourceProvider;

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
}
