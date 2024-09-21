package com.vladmihalcea.hpjp.hibernate.connection;

import com.vladmihalcea.hpjp.util.PersistenceUnitInfoImpl;
import org.hibernate.engine.jdbc.connections.internal.DatasourceConnectionProviderImpl;
import org.hibernate.engine.jdbc.connections.spi.ConnectionProvider;

import java.util.Properties;

public class JPADataSourceConnectionProviderTest extends DriverManagerConnectionProviderTest {

    protected void appendDriverProperties(Properties properties) {

    }

    @Override
    public Class<? extends ConnectionProvider> expectedConnectionProviderClass() {
        return DatasourceConnectionProviderImpl.class;
    }

    @Override
    protected PersistenceUnitInfoImpl persistenceUnitInfo(String name) {
        PersistenceUnitInfoImpl persistenceUnitInfo = super.persistenceUnitInfo(name);
        return persistenceUnitInfo.setNonJtaDataSource(dataSourceProvider().dataSource());
    }

}
