package com.vladmihalcea.hpjp.spring.data.query.fetch.config;

import com.vladmihalcea.hpjp.spring.data.base.config.SpringDataJPABaseConfiguration;
import com.vladmihalcea.hpjp.spring.data.query.fetch.domain.Post;
import io.hypersistence.utils.spring.repository.BaseJpaRepositoryImpl;
import jakarta.persistence.EntityManagerFactory;
import jakarta.persistence.metamodel.EntityType;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.concurrent.ConcurrentMapCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

import java.util.Properties;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 *
 * @author Vlad Mihalcea
 */
@ComponentScan(
    basePackages = {
        "com.vladmihalcea.hpjp.spring.data.query.fetch",
    }
)
@EnableJpaRepositories(
    basePackages = "com.vladmihalcea.hpjp.spring.data.query.fetch.repository",
    repositoryBaseClass = BaseJpaRepositoryImpl.class
)
@EnableCaching
public class SpringDataJPAJoinFetchPaginationConfiguration extends SpringDataJPABaseConfiguration {

    @Override
    protected String packageToScan() {
        return Post.class.getPackageName();
    }

    @Override
    protected void additionalProperties(Properties properties) {
        super.additionalProperties(properties);
        properties.put("hibernate.jdbc.batch_size", "100");
        properties.put("hibernate.order_inserts", "true");
        //properties.put("hibernate.query.fail_on_pagination_over_collection_fetch", "true");
    }

    @Bean
    public CacheManager cacheManager(EntityManagerFactory entityManagerFactory) {
        String[] entityNames = entityManagerFactory
            .getMetamodel()
            .getEntities()
            .stream()
            .map(EntityType::getName)
            .toArray(String[]::new);

        return new ConcurrentMapCacheManager(entityNames);
    }

    @Bean
    public ExecutorService executorService() {
        return Executors.newFixedThreadPool(
            Runtime.getRuntime().availableProcessors()
        );
    }
}
