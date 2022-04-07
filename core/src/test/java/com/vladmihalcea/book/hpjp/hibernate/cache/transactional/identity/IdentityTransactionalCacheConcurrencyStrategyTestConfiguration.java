package com.vladmihalcea.book.hpjp.hibernate.cache.transactional.identity;

import com.vladmihalcea.book.hpjp.util.spring.config.jta.HSQLDBJtaTransactionManagerConfiguration;
import org.springframework.context.annotation.Configuration;

import java.util.Properties;

@Configuration
public class IdentityTransactionalCacheConcurrencyStrategyTestConfiguration extends
		HSQLDBJtaTransactionManagerConfiguration {

    @Override
    protected Properties additionalProperties() {
        Properties properties = super.additionalProperties();
        properties.put("hibernate.cache.region.factory_class", "jcache");
        properties.put("hibernate.generate_statistics", Boolean.TRUE.toString());
        return properties;
    }

    @Override
    protected Class configurationClass() {
        return IdentityTransactionalEntities.class;
    }
}
