package com.vladmihalcea.hpjp.spring.data.crud.config;

import com.vladmihalcea.hpjp.spring.data.base.config.SpringDataJPABaseConfiguration;
import com.vladmihalcea.hpjp.spring.data.crud.domain.Post;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

import java.util.Properties;

/**
 *
 * @author Vlad Mihalcea
 */
@ComponentScan(
    basePackages = {
        "com.vladmihalcea.hpjp.spring.data.crud",
    }
)
@EnableJpaRepositories(
    basePackages = {
        "com.vladmihalcea.hpjp.spring.data.crud.service",
        "com.vladmihalcea.hpjp.spring.data.crud.repository",
        "io.hypersistence.utils.spring.repository"
    }
)
public class SpringDataJPACrudConfiguration extends SpringDataJPABaseConfiguration {

    @Override
    protected String packageToScan() {
        return Post.class.getPackageName();
    }

    @Override
    protected void additionalProperties(Properties properties) {
        super.additionalProperties(properties);
        properties.put("hibernate.jdbc.batch_size", "100");
        properties.put("hibernate.order_inserts", "true");
    }
}
