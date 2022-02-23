package com.vladmihalcea.book.hpjp.spring.data.query.exists.config;

import com.vladmihalcea.book.hpjp.spring.data.base.config.SpringDataJPABaseConfiguration;
import com.vladmihalcea.book.hpjp.spring.data.query.exists.domain.Post;
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
        "com.vladmihalcea.book.hpjp.spring.data.query.exists",
    }
)
@EnableJpaRepositories("com.vladmihalcea.book.hpjp.spring.data.query.exists.repository")
@PropertySource({"/META-INF/jdbc-mysql.properties"})
public class SpringDataJPAExistsConfiguration extends SpringDataJPABaseConfiguration {

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
