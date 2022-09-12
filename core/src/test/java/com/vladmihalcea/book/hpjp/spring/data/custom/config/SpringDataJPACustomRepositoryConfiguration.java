package com.vladmihalcea.book.hpjp.spring.data.custom.config;

import com.vladmihalcea.book.hpjp.spring.data.base.config.SpringDataJPABaseConfiguration;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.PropertySource;
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
        "com.vladmihalcea.spring.repository"
    }
)
@PropertySource({"/META-INF/jdbc-hsqldb.properties"})
public class SpringDataJPACustomRepositoryConfiguration extends SpringDataJPABaseConfiguration {

}
