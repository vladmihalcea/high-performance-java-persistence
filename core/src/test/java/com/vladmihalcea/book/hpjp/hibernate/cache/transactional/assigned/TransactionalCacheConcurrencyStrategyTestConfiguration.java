package com.vladmihalcea.book.hpjp.hibernate.cache.transactional.assigned;

import com.vladmihalcea.book.hpjp.util.spring.config.jta.HSQLDBJtaTransactionManagerConfiguration;
import org.springframework.context.annotation.Configuration;

import java.util.Properties;

@Configuration
public class TransactionalCacheConcurrencyStrategyTestConfiguration extends HSQLDBJtaTransactionManagerConfiguration {

    @Override
    protected Properties additionalProperties() {
        Properties properties = super.additionalProperties();
        properties.put("hibernate.cache.region.factory_class", "jcache");
        //JCache makes it difficult to print the cache content
        /*properties.put("hibernate.cache.region.factory_class", "jcache");
        properties.put("hibernate.javax.cache.provider", "org.ehcache.jsr107.EhcacheCachingProvider");*/
        properties.put("hibernate.generate_statistics", Boolean.TRUE.toString());
        return properties;
    }

    @Override
    protected Class configurationClass() {
        return TransactionalEntities.class;
    }
}
