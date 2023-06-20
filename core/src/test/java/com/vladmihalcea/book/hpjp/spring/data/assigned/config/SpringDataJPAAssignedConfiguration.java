package com.vladmihalcea.book.hpjp.spring.data.assigned.config;

import com.vladmihalcea.book.hpjp.spring.data.assigned.domain.Book;
import com.vladmihalcea.book.hpjp.spring.data.base.config.SpringDataJPABaseConfiguration;
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
        "com.vladmihalcea.book.hpjp.spring.data.assigned",
    }
)
@EnableJpaRepositories(
    basePackages = {
        "com.vladmihalcea.book.hpjp.spring.data.assigned.repository",
        "io.hypersistence.utils.spring.repository"
    },
    repositoryBaseClass = BaseJpaRepositoryImpl.class
)
public class SpringDataJPAAssignedConfiguration extends SpringDataJPABaseConfiguration {

    @Override
    protected String packageToScan() {
        return Book.class.getPackageName();
    }

    @Override
    protected void additionalProperties(Properties properties) {
        properties.put("hibernate.jdbc.batch_size", "100");
        properties.put("hibernate.order_inserts", "true");
    }
}
