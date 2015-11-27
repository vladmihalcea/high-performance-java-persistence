package com.vladmihalcea.book.hpjp.hibernate.connection;

import com.vladmihalcea.book.hpjp.util.PersistenceUnitInfoImpl;

import java.util.Properties;

public class JPADataSourceConnectionProviderTest extends DriverConnectionProviderTest {

    protected void appendDriverProperties(Properties properties) {

    }

    @Override
    protected PersistenceUnitInfoImpl persistenceUnitInfo(String name) {
        PersistenceUnitInfoImpl persistenceUnitInfo = super.persistenceUnitInfo(name);
        return persistenceUnitInfo.setNonJtaDataSource(dataSourceProvider().dataSource());
    }

}
