package com.vladmihalcea.hpjp.hibernate.connection.jta;

import com.vladmihalcea.hpjp.spring.transaction.jta.narayana.config.NarayanaJTATransactionManagerConfiguration;
import org.hibernate.cfg.AvailableSettings;
import org.hibernate.resource.jdbc.spi.PhysicalConnectionHandlingMode;
import org.springframework.context.annotation.Configuration;

import java.util.Properties;

@Configuration
public class NarayanaJTAConnectionReleaseConfiguration extends NarayanaJTATransactionManagerConfiguration {

    protected String[] packagesToScan() {
        return new String[]{
            this.getClass().getPackage().getName()
        };
    }

    @Override
    protected Properties additionalProperties() {
        Properties properties = super.additionalProperties();
        properties.remove(
            "hibernate.session_factory.statement_inspector"
        );
        //properties.put("hibernate.generate_statistics", "true");
        //properties.put("hibernate.stats.factory", TransactionStatisticsFactory.class.getName());

        //properties.setProperty( AvailableSettings.CONNECTION_HANDLING, PhysicalConnectionHandlingMode.DELAYED_ACQUISITION_AND_RELEASE_AFTER_TRANSACTION.name());
        properties.setProperty( AvailableSettings.CONNECTION_HANDLING, PhysicalConnectionHandlingMode.DELAYED_ACQUISITION_AND_RELEASE_AFTER_STATEMENT.name());
        return properties;
    }
}
