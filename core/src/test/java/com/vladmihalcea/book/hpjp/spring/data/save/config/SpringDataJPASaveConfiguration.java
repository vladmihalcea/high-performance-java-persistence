package com.vladmihalcea.book.hpjp.spring.data.save.config;

import com.vladmihalcea.book.hpjp.spring.data.base.config.SpringDataJPABaseConfiguration;
import com.vladmihalcea.book.hpjp.spring.data.save.domain.Post;
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
        "com.vladmihalcea.book.hpjp.spring.data.save",
    }
)
@EnableJpaRepositories(
    basePackages = {
        "com.vladmihalcea.book.hpjp.spring.data.save.service",
        "com.vladmihalcea.book.hpjp.spring.data.save.repository",
        "com.vladmihalcea.spring.repository"
    }
)
@PropertySource({"/META-INF/jdbc-postgresql.properties"})
public class SpringDataJPASaveConfiguration extends SpringDataJPABaseConfiguration {

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
