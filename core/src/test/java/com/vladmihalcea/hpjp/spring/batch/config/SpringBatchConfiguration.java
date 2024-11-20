package com.vladmihalcea.hpjp.spring.batch.config;

import com.vladmihalcea.hpjp.util.DataSourceProxyType;
import com.vladmihalcea.hpjp.util.logging.InlineQueryLogEntryCreator;
import com.vladmihalcea.hpjp.util.providers.DataSourceProvider;
import com.vladmihalcea.hpjp.util.providers.Database;
import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import io.hypersistence.utils.spring.repository.BaseJpaRepositoryImpl;
import jakarta.persistence.EntityManagerFactory;
import net.ttddyy.dsproxy.listener.logging.SLF4JQueryLoggingListener;
import net.ttddyy.dsproxy.support.ProxyDataSourceBuilder;
import org.hibernate.jpa.HibernatePersistenceProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.EnableAspectJAutoProxy;
import org.springframework.context.support.PropertySourcesPlaceholderConfigurer;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.orm.jpa.JpaTransactionManager;
import org.springframework.orm.jpa.JpaVendorAdapter;
import org.springframework.orm.jpa.LocalContainerEntityManagerFactoryBean;
import org.springframework.orm.jpa.vendor.HibernateJpaVendorAdapter;
import org.springframework.transaction.annotation.EnableTransactionManagement;
import org.springframework.transaction.support.TransactionTemplate;

import javax.sql.DataSource;
import java.util.Properties;

/**
 *
 * @author Vlad Mihalcea
 */
@Configuration
@EnableTransactionManagement
@EnableAspectJAutoProxy
@EnableJpaRepositories(
    value = "com.vladmihalcea.hpjp.spring.batch.repository",
    repositoryBaseClass = BaseJpaRepositoryImpl.class
)
@ComponentScan(
    value = {
        "com.vladmihalcea.hpjp.spring.batch.service",
        "io.hypersistence.utils.spring.aop"
    }
)
public class SpringBatchConfiguration {

    public static final String DATA_SOURCE_PROXY_NAME = DataSourceProxyType.DATA_SOURCE_PROXY.name();

    private int maxConnections = 64;

    @Bean
    public static PropertySourcesPlaceholderConfigurer properties() {
        return new PropertySourcesPlaceholderConfigurer();
    }

    @Bean
    public Database database() {
        //return Database.YUGABYTEDB_CLUSTER;
        return Database.POSTGRESQL;
    }

    @Bean
    public DataSourceProvider dataSourceProvider() {
        return database().dataSourceProvider();
    }
    
    public HikariDataSource poolingDataSource() {
        HikariConfig hikariConfig = new HikariConfig();
        hikariConfig.setMaximumPoolSize(maxConnections);
        hikariConfig.setAutoCommit(false);
        hikariConfig.setDataSource(dataSourceProvider().dataSource());
        return new HikariDataSource(hikariConfig);
    }

    @Bean
    public DataSource dataSource() {
        SLF4JQueryLoggingListener loggingListener = new SLF4JQueryLoggingListener();
        loggingListener.setQueryLogEntryCreator(new InlineQueryLogEntryCreator());
        return ProxyDataSourceBuilder
            .create(poolingDataSource())
            .name(DATA_SOURCE_PROXY_NAME)
            .listener(loggingListener)
            .build();
    }

    @Bean
    public LocalContainerEntityManagerFactoryBean entityManagerFactory(
            @Autowired DataSource dataSource) {
        LocalContainerEntityManagerFactoryBean entityManagerFactoryBean = new LocalContainerEntityManagerFactoryBean();
        entityManagerFactoryBean.setPersistenceUnitName(getClass().getSimpleName());
        entityManagerFactoryBean.setDataSource(dataSource);
        entityManagerFactoryBean.setPackagesToScan(packagesToScan());

        JpaVendorAdapter vendorAdapter = new HibernateJpaVendorAdapter();
        entityManagerFactoryBean.setJpaVendorAdapter(vendorAdapter);
        entityManagerFactoryBean.setJpaProperties(additionalProperties());
        return entityManagerFactoryBean;
    }

    @Bean
    public JpaTransactionManager transactionManager(EntityManagerFactory entityManagerFactory){
        JpaTransactionManager transactionManager = new JpaTransactionManager();
        transactionManager.setEntityManagerFactory(entityManagerFactory);
        return transactionManager;
    }

    @Bean
    public TransactionTemplate transactionTemplate(EntityManagerFactory entityManagerFactory) {
        return new TransactionTemplate(transactionManager(entityManagerFactory));
    }

    @Bean
    public Integer batchProcessingSize() {
        return 100;
    }

    protected Properties additionalProperties() {
        Properties properties = new Properties();
        properties.setProperty("hibernate.hbm2ddl.auto", "create-drop");
        properties.setProperty("hibernate.jdbc.batch_size", String.valueOf(batchProcessingSize()));
        properties.setProperty("hibernate.order_inserts", "true");
        properties.setProperty("hibernate.order_updates", "true");
        return properties;
    }

    protected String[] packagesToScan() {
        return new String[]{
            "com.vladmihalcea.hpjp.spring.batch.domain"
        };
    }
}
