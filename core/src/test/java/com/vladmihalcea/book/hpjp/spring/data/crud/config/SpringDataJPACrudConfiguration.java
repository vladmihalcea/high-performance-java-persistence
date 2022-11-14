package com.vladmihalcea.book.hpjp.spring.data.crud.config;

import com.vladmihalcea.book.hpjp.spring.data.base.config.SpringDataJPABaseConfiguration;
import com.vladmihalcea.book.hpjp.spring.data.crud.domain.Post;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.PropertySource;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

import java.util.Properties;

/**
 *
 * @author Vlad Mihalcea
 */
@ComponentScan(
    basePackages = {
        "com.vladmihalcea.book.hpjp.spring.data.crud",
    }
)
@EnableJpaRepositories(
    basePackages = {
        "com.vladmihalcea.book.hpjp.spring.data.crud.service",
        "com.vladmihalcea.book.hpjp.spring.data.crud.repository",
        "com.vladmihalcea.spring.repository"
    }
)
@PropertySource({"/META-INF/jdbc-postgresql.properties"})
public class SpringDataJPACrudConfiguration extends SpringDataJPABaseConfiguration {

    @Override
    protected String packageToScan() {
        return Post.class.getPackageName();
    }

    @Override
    protected void additionalProperties(Properties properties) {
        properties.put("hibernate.jdbc.batch_size", "100");
        properties.put("hibernate.order_inserts", "true");
    }
}
