package com.vladmihalcea.book.hpjp.hibernate.connection.jta;

import com.vladmihalcea.book.hpjp.hibernate.statistics.TransactionStatisticsFactory;
import com.vladmihalcea.book.hpjp.util.spring.config.jta.PostgreSQLJTATransactionManagerConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.DependsOn;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.springframework.orm.jpa.JpaVendorAdapter;
import org.springframework.orm.jpa.LocalContainerEntityManagerFactoryBean;
import org.springframework.orm.jpa.vendor.HibernateJpaVendorAdapter;

import javax.sql.DataSource;
import java.util.Properties;

@Configuration
public class JTAMultipleTransactionsConfiguration extends PostgreSQLJTATransactionManagerConfiguration {

    @Override
    protected Class configurationClass() {
        return FlexyPoolEntities.class;
    }

    @Override
    protected Properties additionalProperties() {
        Properties properties = super.additionalProperties();
        properties.put("hibernate.generate_statistics", "true");
        properties.put("hibernate.stats.factory", TransactionStatisticsFactory.class.getName());

        //properties.setProperty("hibernate.connection.release_mode", "after_transaction");
        properties.setProperty("hibernate.connection.release_mode", "after_statement");
        return properties;
    }

    @Bean
    public LocalContainerEntityManagerFactoryBean extraEntityManagerFactory() {
        LocalContainerEntityManagerFactoryBean localContainerEntityManagerFactoryBean = new LocalContainerEntityManagerFactoryBean();
        localContainerEntityManagerFactoryBean.setJtaDataSource(extraDataSource());
        localContainerEntityManagerFactoryBean.setPackagesToScan(packagesToScan());

        JpaVendorAdapter vendorAdapter = new HibernateJpaVendorAdapter();
        localContainerEntityManagerFactoryBean.setJpaVendorAdapter(vendorAdapter);
        localContainerEntityManagerFactoryBean.setJpaProperties(additionalProperties());
        return localContainerEntityManagerFactoryBean;
    }

    public DataSource extraDataSource() {
        DriverManagerDataSource dataSource = new DriverManagerDataSource();
        dataSource.setDriverClassName(dataSourceClassName);
        dataSource.setUrl(
            String.format(
                "jdbc:postgresql://%s:%s/%s",
                jdbcHost,
                jdbcPort,
                jdbcDatabase
            )
        );
        dataSource.setUsername(jdbcUser);
        dataSource.setPassword(jdbcPassword);
        return dataSource;
    }
}
