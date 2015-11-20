package com.vladmihalcea.book.hpjp.hibernate.connection;

import com.vladmihalcea.book.hpjp.util.AbstractTest;
import com.vladmihalcea.book.hpjp.util.providers.BlogEntityProvider;
import org.junit.Test;

import javax.sql.DataSource;
import java.util.Properties;
import java.util.concurrent.atomic.AtomicLong;

public class DriverConnectionProviderTest extends AbstractTest {

    private BlogEntityProvider entityProvider = new BlogEntityProvider();

    @Override
    protected Class<?>[] entities() {
        return entityProvider.entities();
    }

    protected DataSource newDataSource() {
        return null;
    }

    protected Properties properties() {
        Properties properties = super.properties();
        properties.put("hibernate.hbm2ddl.auto", "update");
        properties.put("hibernate.show_sql", "true");

        appendDriverProperties(properties);
        return properties;
    }

    protected void appendDriverProperties(Properties properties) {
        DataSourceProvider dataSourceProvider = dataSourceProvider();
        properties.put("hibernate.connection.driver_class", "org.hsqldb.jdbcDriver");
        properties.put("hibernate.connection.url", dataSourceProvider.url());
        properties.put("hibernate.connection.username", dataSourceProvider.username());
        properties.put("hibernate.connection.password", dataSourceProvider.password());
    }

    @Test
    public void testConnection() {
        for (final AtomicLong i = new AtomicLong(); i.get() < 5; i.incrementAndGet()) {
            doInJPA(em -> {
                em.persist(new BlogEntityProvider.Post(i.get()));
            });
        }
    }
}
