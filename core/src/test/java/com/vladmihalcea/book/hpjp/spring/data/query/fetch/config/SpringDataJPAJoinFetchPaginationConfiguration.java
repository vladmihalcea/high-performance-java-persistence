package com.vladmihalcea.book.hpjp.spring.data.query.fetch.config;

import com.blazebit.persistence.Criteria;
import com.blazebit.persistence.CriteriaBuilderFactory;
import com.blazebit.persistence.spi.CriteriaBuilderConfiguration;
import com.vladmihalcea.book.hpjp.spring.data.base.config.SpringDataJPABaseConfiguration;
import com.vladmihalcea.book.hpjp.spring.data.query.fetch.domain.Post;
import io.hypersistence.utils.spring.repository.BaseJpaRepositoryImpl;
import jakarta.persistence.EntityManagerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

import java.util.Properties;

/**
 *
 * @author Vlad Mihalcea
 */
@ComponentScan(
    basePackages = {
        "com.vladmihalcea.book.hpjp.spring.data.query.fetch",
    }
)
@EnableJpaRepositories(
    basePackages = "com.vladmihalcea.book.hpjp.spring.data.query.fetch.repository",
    repositoryBaseClass = BaseJpaRepositoryImpl.class
)
public class SpringDataJPAJoinFetchPaginationConfiguration extends SpringDataJPABaseConfiguration {

    @Override
    protected String packageToScan() {
        return Post.class.getPackageName();
    }

    @Override
    protected void additionalProperties(Properties properties) {
        properties.put("hibernate.jdbc.batch_size", "100");
        properties.put("hibernate.order_inserts", "true");
    }

    @Bean
    public CriteriaBuilderFactory criteriaBuilderFactory(EntityManagerFactory entityManagerFactory) {
        CriteriaBuilderConfiguration config = Criteria.getDefault();
        return config.createCriteriaBuilderFactory(entityManagerFactory);
    }
}
