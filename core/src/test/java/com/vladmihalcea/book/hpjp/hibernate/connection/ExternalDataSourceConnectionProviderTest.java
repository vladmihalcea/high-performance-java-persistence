package com.vladmihalcea.book.hpjp.hibernate.connection;

public class ExternalDataSourceConnectionProviderTest {

    /*protected Properties getProperties() {
        Properties properties = new Properties();
        properties.put("hibernate.dialect", "org.hibernate.dialect.HSQLDialect");
        //log settings
        properties.put("hibernate.hbm2ddl.auto", "update");
        //data source settings
        properties.put("hibernate.connection.datasource", newDataSource());
        return properties;
    }

    @Override
    protected SessionFactory newSessionFactory() {
        Properties properties = getProperties();

        return new Configuration()
                .addProperties(properties)
                .addAnnotatedClass(SecurityId.class)
                .buildSessionFactory(
                        new StandardServiceRegistryBuilder()
                                .applySettings(properties)
                                .build()
                );
    }

    protected ProxyDataSource newDataSource() {
        JDBCDataSource actualDataSource = new JDBCDataSource();
        actualDataSource.setUrl("jdbc:hsqldb:mem:test");
        actualDataSource.setUser("sa");
        actualDataSource.setPassword("");
        ProxyDataSource proxyDataSource = new ProxyDataSource();
        proxyDataSource.setDataSource(actualDataSource);
        proxyDataSource.setListener(new SLF4JQueryLoggingListener());
        return proxyDataSource;
    }*/



}
