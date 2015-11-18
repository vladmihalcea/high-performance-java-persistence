package com.vladmihalcea.book.hpjp.hibernate.connection;

import com.vladmihalcea.book.hpjp.util.AbstractTest;
import com.vladmihalcea.book.hpjp.util.PersistenceUnitInfoImpl;
import com.vladmihalcea.book.hpjp.util.providers.BlogEntityProvider;
import org.junit.Test;

import javax.persistence.spi.PersistenceUnitInfo;
import javax.sql.DataSource;
import java.util.Properties;
import java.util.concurrent.atomic.AtomicLong;

public class JPADriverConnectionProviderTest extends DriverConnectionProviderTest {

    protected void appendDriverProperties(Properties properties) {
        DataSourceProvider dataSourceProvider = dataSourceProvider();
        properties.put("javax.persistence.jdbc.driver", "org.hsqldb.jdbcDriver");
        properties.put("javax.persistence.jdbc.url", dataSourceProvider.url());
        properties.put("javax.persistence.jdbc.user", dataSourceProvider.username());
        properties.put("javax.persistence.jdbc.password", dataSourceProvider.password());
    }
}
