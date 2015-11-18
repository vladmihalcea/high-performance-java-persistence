package com.vladmihalcea.book.hpjp.hibernate.connection;

import java.util.Properties;

public class JPADataSourceProxyConnectionProviderTest extends JPADataSourceConnectionProviderTest {

    protected void appendDriverProperties(Properties properties) {
        properties.put("hibernate.connection.provider_class", DataSourceProxyConnectionProvider.class.getName());
    }
}
