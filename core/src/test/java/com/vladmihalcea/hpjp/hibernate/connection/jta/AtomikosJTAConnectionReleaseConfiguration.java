package com.vladmihalcea.hpjp.hibernate.connection.jta;

import com.vladmihalcea.hpjp.util.spring.config.jta.PostgreSQLJTATransactionManagerConfiguration;
import org.hibernate.cfg.AvailableSettings;
import org.hibernate.resource.jdbc.spi.PhysicalConnectionHandlingMode;
import org.springframework.context.annotation.Configuration;

import java.util.Properties;

@Configuration
public class AtomikosJTAConnectionReleaseConfiguration extends PostgreSQLJTATransactionManagerConfiguration {

    @Override
    protected Class configurationClass() {
        return this.getClass();
    }

    @Override
    protected Properties additionalProperties() {
        Properties properties = super.additionalProperties();
        //properties.put("hibernate.generate_statistics", "true");
        //properties.put("hibernate.stats.factory", TransactionStatisticsFactory.class.getName());

        properties.setProperty( AvailableSettings.CONNECTION_HANDLING, PhysicalConnectionHandlingMode.DELAYED_ACQUISITION_AND_RELEASE_AFTER_TRANSACTION.name());
        //properties.setProperty( AvailableSettings.CONNECTION_HANDLING, PhysicalConnectionHandlingMode.DELAYED_ACQUISITION_AND_RELEASE_AFTER_STATEMENT.name());
        return properties;
    }
}

