package com.vladmihalcea.hpjp.hibernate.cache.transactional.assigned;

import com.vladmihalcea.hpjp.spring.transaction.jta.narayana.config.NarayanaJTATransactionManagerConfiguration;
import org.ehcache.jsr107.EhcacheCachingProvider;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.DependsOn;

import javax.cache.CacheManager;
import java.net.URISyntaxException;
import java.util.Properties;

@Configuration
public class TransactionalCacheConcurrencyStrategyTestConfiguration extends NarayanaJTATransactionManagerConfiguration {

    @Override
    protected Properties additionalProperties() {
        Properties properties = super.additionalProperties();
        properties.put("hibernate.cache.region.factory_class", "jcache");
        properties.put("hibernate.javax.cache.cache_manager", cacheManager());
        properties.put("hibernate.generate_statistics", Boolean.TRUE.toString());
        properties.put("hibernate.cache.use_structured_entries", Boolean.FALSE.toString());
        return properties;
    }

    @Bean
    @DependsOn("transactionManager")
    public CacheManager cacheManager() {
        try {
            return new EhcacheCachingProvider().getCacheManager(
                getClass().getResource("/ehcache.xml").toURI(),
                getClass().getClassLoader()
            );
        } catch (URISyntaxException e) {
            throw new RuntimeException(e);
        }
    }

    protected String[] packagesToScan() {
        return new String[]{
            TransactionalEntities.class.getPackage().getName()
        };
    }
}
