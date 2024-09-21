package com.vladmihalcea.hpjp.hibernate.connection;

import com.vladmihalcea.hpjp.util.AbstractTest;
import com.vladmihalcea.hpjp.util.providers.entity.BlogEntityProvider;
import org.hibernate.engine.jdbc.connections.spi.ConnectionProvider;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.junit.Test;

import javax.sql.DataSource;
import java.util.Properties;

import static org.junit.Assert.assertTrue;

public abstract class AbstractConnectionProviderTest extends AbstractTest {

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
        properties.put("hibernate.hbm2ddl.auto", "create-drop");
        appendDriverProperties(properties);
        return properties;
    }

    protected abstract void appendDriverProperties(Properties properties);

    @Test
    public void testConnectionProvider() {
        SessionFactoryImplementor sessionFactory = entityManagerFactory()
            .unwrap(SessionFactoryImplementor.class);

        ConnectionProvider connectionProvider = sessionFactory.getServiceRegistry()
            .getService(ConnectionProvider.class);
        assertTrue(expectedConnectionProviderClass().isInstance(connectionProvider));
    }

    public abstract Class<? extends ConnectionProvider> expectedConnectionProviderClass();
}
