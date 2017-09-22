package com.vladmihalcea.book.hpjp.hibernate.connection.jta;

import java.util.Properties;

import org.hibernate.cfg.AvailableSettings;

import com.vladmihalcea.book.hpjp.hibernate.statistics.TransactionStatisticsFactory;
import com.vladmihalcea.book.hpjp.util.spring.config.jta.PostgreSQLJTATransactionManagerConfiguration;
import org.springframework.context.annotation.Configuration;

@Configuration
public class HandlingModeJTAConnectionReleaseConfiguration extends PostgreSQLJTATransactionManagerConfiguration {

    @Override
    protected Class configurationClass() {
        return this.getClass();
    }

    @Override
    protected Properties additionalProperties() {
        Properties properties = super.additionalProperties();
        properties.put("hibernate.generate_statistics", "true");
        properties.put("hibernate.stats.factory", TransactionStatisticsFactory.class.getName());

        properties.setProperty( AvailableSettings.CONNECTION_HANDLING, "delayed_acquisition_and_release_after_transaction");
        return properties;
    }
}
