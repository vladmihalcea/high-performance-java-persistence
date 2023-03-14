package com.vladmihalcea.book.hpjp.spring.data.base.config;

import io.hypersistence.utils.spring.repository.BaseJpaRepositoryImpl;
import org.springframework.context.annotation.ComponentScan;
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
    value = "com.vladmihalcea.book.hpjp.spring.data.base.repository",
    repositoryBaseClass = BaseJpaRepositoryImpl.class
)
public class SpringDataJPABaseRepositoryConfiguration extends SpringDataJPABaseConfiguration {

}
