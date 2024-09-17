package com.vladmihalcea.hpjp.spring.data.audit.config;

import com.vladmihalcea.hpjp.spring.data.audit.domain.Post;
import com.vladmihalcea.hpjp.spring.data.base.config.SpringDataJPABaseConfiguration;
import org.hibernate.envers.configuration.EnversSettings;
import org.hibernate.envers.strategy.internal.ValidityAuditStrategy;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.data.envers.repository.support.EnversRevisionRepositoryFactoryBean;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

import java.util.Properties;

/**
 *
 * @author Vlad Mihalcea
 */
@ComponentScan(
    basePackages = {
        "com.vladmihalcea.hpjp.spring.data.audit",
    }
)
@EnableJpaRepositories(
    basePackages = {
        "com.vladmihalcea.hpjp.spring.data.audit.service",
        "com.vladmihalcea.hpjp.spring.data.audit.repository",
        "io.hypersistence.utils.spring.repository"
    },
    repositoryFactoryBeanClass = EnversRevisionRepositoryFactoryBean.class
)
public class SpringDataJPAAuditConfiguration extends SpringDataJPABaseConfiguration {

    @Override
    protected String packageToScan() {
        return Post.class.getPackageName();
    }

    @Override
    protected void additionalProperties(Properties properties) {
        super.additionalProperties(properties);
        properties.put("hibernate.jdbc.batch_size", "100");
        properties.put("hibernate.order_inserts", "true");
        properties.setProperty(
            EnversSettings.AUDIT_STRATEGY,
            ValidityAuditStrategy.class.getName()
        );
    }
}
