package com.vladmihalcea.book.hpjp.hibernate.connection;

import java.util.Properties;

public class C3P0JPAConnectionProviderTest extends DriverConnectionProviderTest {

    @Override
    protected void appendDriverProperties(Properties properties) {
        super.appendDriverProperties(properties);
        properties.put("hibernate.c3p0.min_size", 1);
        properties.put("hibernate.c3p0.max_size", 5);
    }
}
