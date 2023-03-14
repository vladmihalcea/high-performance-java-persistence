package com.vladmihalcea.book.hpjp.spring.data.custom.config;

import com.vladmihalcea.book.hpjp.spring.data.base.config.SpringDataJPABaseConfiguration;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

/**
 *
 * @author Vlad Mihalcea
 */
@ComponentScan(
    basePackages = {
        "com.vladmihalcea.book.hpjp.spring.data.custom.service",
    }
)
@EnableJpaRepositories(
    basePackages = {
        "com.vladmihalcea.book.hpjp.spring.data.custom.repository",
        "io.hypersistence.utils.spring.repository"
    }
)
public class SpringDataJPACustomRepositoryConfiguration extends SpringDataJPABaseConfiguration {

}
