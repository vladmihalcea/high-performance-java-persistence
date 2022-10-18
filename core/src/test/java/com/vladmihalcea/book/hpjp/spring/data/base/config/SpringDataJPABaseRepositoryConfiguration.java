package com.vladmihalcea.book.hpjp.spring.data.base.config;

import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.PropertySource;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

/**
 *
 * @author Vlad Mihalcea
 */
@ComponentScan(
    basePackages = {
        "com.vladmihalcea.book.hpjp.spring.data.base.service",
    }
)
@EnableJpaRepositories(
    basePackages = {
        "com.vladmihalcea.book.hpjp.spring.data.base.repository"
    }
)
@PropertySource({"/META-INF/jdbc-hsqldb.properties"})
public class SpringDataJPABaseRepositoryConfiguration extends SpringDataJPABaseConfiguration {

}
