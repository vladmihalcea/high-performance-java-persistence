package com.vladmihalcea.hpjp.spring.data.bytecode.config;

import com.vladmihalcea.hpjp.spring.data.base.config.SpringDataJPABaseConfiguration;
import com.vladmihalcea.hpjp.util.providers.Database;
import org.hibernate.cfg.BytecodeSettings;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.EnableLoadTimeWeaving;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

import java.util.Properties;

/**
 *
 * @author Vlad Mihalcea
 */
@ComponentScan(
    basePackages = {
        "com.vladmihalcea.hpjp.spring.data.bytecode",
    }
)
@EnableJpaRepositories(
    basePackages = {
        "com.vladmihalcea.hpjp.spring.data.bytecode.repository",
    }
)
@EnableLoadTimeWeaving
public class SpringDataJPARuntimeBytecodeEnhancementConfiguration extends SpringDataJPABaseConfiguration {

    @Override
    protected String packageToScan() {
        return "com.vladmihalcea.hpjp.hibernate.forum";
    }

    public Database database() {
        return Database.MYSQL;
    }

    @Override
    protected void additionalProperties(Properties properties) {
        super.additionalProperties(properties);
        properties.put("hibernate.jdbc.batch_size", "100");
        properties.put("hibernate.order_inserts", "true");
        properties.put(BytecodeSettings.ENHANCER_ENABLE_DIRTY_TRACKING, "false");
    }
}
