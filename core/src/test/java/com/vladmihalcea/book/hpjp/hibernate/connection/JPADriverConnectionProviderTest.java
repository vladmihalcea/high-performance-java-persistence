package com.vladmihalcea.book.hpjp.hibernate.connection;

import java.util.Properties;

import com.vladmihalcea.book.hpjp.util.providers.DataSourceProvider;

public class JPADriverConnectionProviderTest extends DriverConnectionProviderTest {

    protected void appendDriverProperties(Properties properties) {
        DataSourceProvider dataSourceProvider = dataSourceProvider();
        properties.put("javax.persistence.jdbc.driver", "org.hsqldb.jdbcDriver");
        properties.put("javax.persistence.jdbc.url", dataSourceProvider.url());
        properties.put("javax.persistence.jdbc.user", dataSourceProvider.username());
        properties.put("javax.persistence.jdbc.password", dataSourceProvider.password());
    }
}
