package com.vladmihalcea.hpjp.spring.data.record.config;

import com.vladmihalcea.hpjp.spring.data.base.config.SpringDataJPABaseConfiguration;
import com.vladmihalcea.hpjp.spring.data.record.domain.Post;
import io.hypersistence.utils.spring.repository.BaseJpaRepositoryImpl;
import org.hibernate.cfg.AvailableSettings;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

import java.util.Properties;

/**
 *
 * @author Vlad Mihalcea
 */
@ComponentScan(
    basePackages = {
        "com.vladmihalcea.hpjp.spring.data.record",
    }
)
@EnableJpaRepositories(
    basePackages = "com.vladmihalcea.hpjp.spring.data.record.repository",
    repositoryBaseClass = BaseJpaRepositoryImpl.class
)
public class SpringDataJPARecordConfiguration extends SpringDataJPABaseConfiguration {

    @Override
    protected String packageToScan() {
        return Post.class.getPackageName();
    }

    @Override
    protected void additionalProperties(Properties properties) {
        super.additionalProperties(properties);
        properties.put(
            AvailableSettings.STATEMENT_BATCH_SIZE,
            50
        );
        properties.put(
            AvailableSettings.ORDER_INSERTS,
            Boolean.TRUE
        );
    }
}
