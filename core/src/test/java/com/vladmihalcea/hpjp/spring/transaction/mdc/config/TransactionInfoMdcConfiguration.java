package com.vladmihalcea.hpjp.spring.transaction.mdc.config;

import com.vladmihalcea.hpjp.spring.common.config.CommonSpringConfiguration;
import com.vladmihalcea.hpjp.spring.transaction.mdc.event.TransactionInfoSessionEventListener;
import com.vladmihalcea.hpjp.util.providers.Database;
import org.hibernate.cfg.AvailableSettings;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.Properties;

/**
 *
 * @author Vlad Mihalcea
 */
@Configuration
public class TransactionInfoMdcConfiguration extends CommonSpringConfiguration {

    @Bean
    public Database database() {
        return Database.POSTGRESQL;
    }

    @Override
    protected void additionalProperties(Properties properties) {
        super.additionalProperties(properties);
        properties.put(
            AvailableSettings.AUTO_SESSION_EVENTS_LISTENER,
            TransactionInfoSessionEventListener.class.getName()
        );
    }
}
