package com.vladmihalcea.hpjp.spring.transaction.readonly.config;

import com.vladmihalcea.hpjp.spring.data.base.config.SpringDataJPABaseConfiguration;
import com.vladmihalcea.hpjp.spring.transaction.readonly.config.stats.SpringTransactionStatisticsFactory;
import com.vladmihalcea.hpjp.spring.transaction.readonly.domain.Product;
import org.hibernate.cfg.AvailableSettings;
import org.hibernate.cfg.StatisticsSettings;
import org.hibernate.resource.jdbc.spi.PhysicalConnectionHandlingMode;
import org.hibernate.stat.internal.StatisticsInitiator;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.web.client.RestTemplate;

import java.util.Properties;

/**
 *
 * @author Vlad Mihalcea
 */
@ComponentScan(
    basePackages = {
        "com.vladmihalcea.hpjp.spring.transaction.readonly",
    }
)
@EnableJpaRepositories(
    basePackages = {
        "com.vladmihalcea.hpjp.spring.transaction.readonly.repository"
    }
)
public class SpringDataJPAReadOnlyConfiguration extends SpringDataJPABaseConfiguration {

    @Override
    protected String packageToScan() {
        return Product.class.getPackageName();
    }

    @Bean
    public RestTemplate restTemplate() {
        return new RestTemplate();
    }

    @Override
    protected void additionalProperties(Properties properties) {
        super.additionalProperties(properties);
        properties.setProperty(
            AvailableSettings.CONNECTION_PROVIDER_DISABLES_AUTOCOMMIT,
            Boolean.TRUE.toString()
        );
        properties.setProperty(
            AvailableSettings.GENERATE_STATISTICS,
            Boolean.TRUE.toString()
        );
        properties.setProperty(
            StatisticsSettings.STATS_BUILDER,
            SpringTransactionStatisticsFactory.class.getName()
        );
        properties.setProperty(
            AvailableSettings.CONNECTION_HANDLING,
            PhysicalConnectionHandlingMode.DELAYED_ACQUISITION_AND_RELEASE_AFTER_TRANSACTION.name()
        );
    }
}
