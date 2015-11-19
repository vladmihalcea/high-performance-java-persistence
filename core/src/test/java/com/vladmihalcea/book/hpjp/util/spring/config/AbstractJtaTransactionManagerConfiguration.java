package com.vladmihalcea.book.hpjp.util.spring.config;

import bitronix.tm.BitronixTransactionManager;
import bitronix.tm.TransactionManagerServices;
import bitronix.tm.resource.jdbc.PoolingDataSource;
import net.ttddyy.dsproxy.listener.SLF4JQueryLoggingListener;
import net.ttddyy.dsproxy.support.ProxyDataSourceBuilder;
import org.hibernate.engine.transaction.jta.platform.internal.BitronixJtaPlatform;
import org.hsqldb.jdbc.pool.JDBCXADataSource;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.*;
import org.springframework.context.support.PropertySourcesPlaceholderConfigurer;
import org.springframework.core.annotation.Order;
import org.springframework.orm.jpa.JpaVendorAdapter;
import org.springframework.orm.jpa.LocalContainerEntityManagerFactoryBean;
import org.springframework.orm.jpa.vendor.HibernateJpaVendorAdapter;
import org.springframework.transaction.annotation.EnableTransactionManagement;
import org.springframework.transaction.jta.JtaTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import javax.sql.DataSource;
import java.util.Properties;

/**
 * <code>JPAConfig</code> - JPAConfig
 *
 * @author Vlad Mihalcea
 */
@EnableTransactionManagement
@EnableAspectJAutoProxy
public abstract class AbstractJtaTransactionManagerConfiguration {

    @Value("${btm.config.journal:disk}")
    private String btmJournal;

    @Value("${hibernate.dialect}")
    private String hibernateDialect;

    @Bean
    public static PropertySourcesPlaceholderConfigurer properties() {
        return new PropertySourcesPlaceholderConfigurer();
    }

    @Bean
    public bitronix.tm.Configuration btmConfig() {
        bitronix.tm.Configuration configuration = TransactionManagerServices.getConfiguration();
        configuration.setServerId("spring-btm");
        configuration.setWarnAboutZeroResourceTransaction(true);
        configuration.setJournal(btmJournal);
        return configuration;
    }

    @Bean(initMethod = "init", destroyMethod = "close")
    public abstract DataSource actualDataSource();

    @DependsOn(value = "btmConfig, actualDataSource")
    public DataSource dataSource() {
        return ProxyDataSourceBuilder
                .create(actualDataSource())
                .name(getClass().getSimpleName())
                .listener(new SLF4JQueryLoggingListener())
                .build();
    }

    @Bean
    @DependsOn("btmConfig")
    public LocalContainerEntityManagerFactoryBean entityManagerFactory() {
        LocalContainerEntityManagerFactoryBean localContainerEntityManagerFactoryBean = new LocalContainerEntityManagerFactoryBean();
        localContainerEntityManagerFactoryBean.setJtaDataSource(dataSource());
        localContainerEntityManagerFactoryBean.setPackagesToScan(new String[] { "com.vladmihalcea" });

        JpaVendorAdapter vendorAdapter = new HibernateJpaVendorAdapter();
        localContainerEntityManagerFactoryBean.setJpaVendorAdapter(vendorAdapter);
        localContainerEntityManagerFactoryBean.setJpaProperties(additionalProperties());
        return localContainerEntityManagerFactoryBean;
    }

    @Bean(destroyMethod = "shutdown")
    @DependsOn(value = "btmConfig")
    public BitronixTransactionManager jtaTransactionManager() {
        return TransactionManagerServices.getTransactionManager();
    }

    @Bean
    public JtaTransactionManager transactionManager() {
        BitronixTransactionManager bitronixTransactionManager = jtaTransactionManager();
        JtaTransactionManager transactionManager = new JtaTransactionManager();
        transactionManager.setTransactionManager(bitronixTransactionManager);
        transactionManager.setUserTransaction(bitronixTransactionManager);
        transactionManager.setAllowCustomIsolationLevels(true);
        return transactionManager;
    }

    @Bean
    public TransactionTemplate transactionTemplate() {
        return new TransactionTemplate(transactionManager());
    }

    protected Properties additionalProperties() {
        Properties properties = new Properties();

        properties.setProperty("hibernate.archive.autodetection", "class");
        properties.setProperty("hibernate.transaction.jta.platform", BitronixJtaPlatform.class.getName());
        properties.setProperty("hibernate.dialect", hibernateDialect);
        properties.setProperty("hibernate.hbm2ddl.auto", "create-drop");
        properties.setProperty("hibernate.connection.release_mode", "after_transaction");
        //properties.setProperty("hibernate.connection.release_mode", "after_statement");

        return properties;
    }
}
