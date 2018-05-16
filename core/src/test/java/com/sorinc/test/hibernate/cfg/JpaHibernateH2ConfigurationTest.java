package com.sorinc.test.hibernate.cfg;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;
import org.springframework.orm.jpa.JpaTransactionManager;
import org.springframework.orm.jpa.LocalContainerEntityManagerFactoryBean;
import org.springframework.orm.jpa.vendor.HibernateJpaVendorAdapter;
import org.springframework.transaction.annotation.EnableTransactionManagement;

import javax.persistence.EntityManagerFactory;
import java.util.Properties;

@Configuration
@EnableTransactionManagement
public class JpaHibernateH2ConfigurationTest {


    @Bean(name = "transactionManager")
    JpaTransactionManager transactionManager(EntityManagerFactory entityManagerFactory) {
        JpaTransactionManager transactionManager = new JpaTransactionManager();
        transactionManager.setEntityManagerFactory(entityManagerFactory);

        return transactionManager;
    }

    @Bean(name = "entityManagerFactory")
    public LocalContainerEntityManagerFactoryBean entityManagerFactory(Environment environment) {
        LocalContainerEntityManagerFactoryBean entityManagerFactoryBean = new LocalContainerEntityManagerFactoryBean();
        entityManagerFactoryBean.setJpaVendorAdapter(this.vendorAdaptor());
        entityManagerFactoryBean.setPackagesToScan("ro.hpm.hcs.aaa.provider.persistence");
        entityManagerFactoryBean.setJpaProperties(this.jpaHibernateProperties(environment));
        entityManagerFactoryBean.setPersistenceUnitName("test_pu");

        return entityManagerFactoryBean;
    }

    private HibernateJpaVendorAdapter vendorAdaptor() {
        HibernateJpaVendorAdapter hibernateJpaVendorAdapter = new HibernateJpaVendorAdapter();
        hibernateJpaVendorAdapter.setShowSql(true);
        return hibernateJpaVendorAdapter;
    }

    private Properties jpaHibernateProperties(Environment environment) {
        Properties jpaProperties = new Properties();
        jpaProperties.setProperty("javax.persistence.jdbc.url", "jdbc:h2:mem:test;INIT=RUNSCRIPT FROM 'classpath:db/create.sql'\\;RUNSCRIPT FROM 'classpath:db/data.sql'");
        jpaProperties.setProperty("javax.persistence.jdbc.driver", "org.h2.Driver");
        jpaProperties.setProperty("hibernate.dialect", "ro.hpm.hcs.aaa.provider.persistence.hibernate.H2DialectTest");
//        jpaProperties.setProperty("hibernate.hbm2ddl.auto", "validate");
        jpaProperties.setProperty("hibernate.format_sql", "true");
        jpaProperties.setProperty("hibernate.show_sql", "true");

        return jpaProperties;
    }
}
