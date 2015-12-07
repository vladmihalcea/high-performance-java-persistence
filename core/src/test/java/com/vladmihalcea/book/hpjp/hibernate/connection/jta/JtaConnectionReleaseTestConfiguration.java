package com.vladmihalcea.book.hpjp.hibernate.connection.jta;

import com.vladmihalcea.book.hpjp.hibernate.statistics.TransactionStatisticsFactory;
import com.vladmihalcea.book.hpjp.util.spring.config.PostgreSQLJtaTransactionManagerConfiguration;
import org.springframework.context.annotation.Configuration;

import java.util.Properties;

import static org.junit.Assert.assertNotNull;

@Configuration
public class JtaConnectionReleaseTestConfiguration extends PostgreSQLJtaTransactionManagerConfiguration {

    @Override
    protected Class entityClass() {
        return JtaConnectionReleaseTestConfiguration.class;
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
}
