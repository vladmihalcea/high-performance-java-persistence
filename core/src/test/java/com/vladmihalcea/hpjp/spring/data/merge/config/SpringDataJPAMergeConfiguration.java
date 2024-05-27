package com.vladmihalcea.hpjp.spring.data.merge.config;

import com.vladmihalcea.hpjp.spring.data.base.config.SpringDataJPABaseConfiguration;
import com.vladmihalcea.hpjp.spring.data.merge.domain.Post;
import io.hypersistence.utils.spring.repository.BaseJpaRepositoryImpl;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

import java.util.Properties;

/**
 *
 * @author Vlad Mihalcea
 */
@ComponentScan(
    basePackages = {
        "com.vladmihalcea.hpjp.spring.data.merge"
    }
)
@EnableJpaRepositories(
    basePackages = "com.vladmihalcea.hpjp.spring.data.merge.repository",
    repositoryBaseClass = BaseJpaRepositoryImpl.class
)
public class SpringDataJPAMergeConfiguration extends SpringDataJPABaseConfiguration {

    @Override
    protected String packageToScan() {
        return Post.class.getPackageName();
    }

    @Override
    protected void additionalProperties(Properties properties) {
        super.additionalProperties(properties);
        properties.put("hibernate.jdbc.batch_size", "100");
        properties.put("hibernate.order_inserts", "true");
        properties.put("hibernate.order_updates", "true");
    }
}
