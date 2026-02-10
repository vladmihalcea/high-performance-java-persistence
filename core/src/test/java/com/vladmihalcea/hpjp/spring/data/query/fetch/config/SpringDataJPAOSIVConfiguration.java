package com.vladmihalcea.hpjp.spring.data.query.fetch.config;

import org.hibernate.cfg.AvailableSettings;
import org.hibernate.resource.jdbc.spi.PhysicalConnectionHandlingMode;

import java.util.Properties;

/**
 *
 * @author Vlad Mihalcea
 */
public class SpringDataJPAOSIVConfiguration extends SpringDataJPAJoinFetchPaginationConfiguration {

    @Override
    protected void additionalProperties(Properties properties) {
        super.additionalProperties(properties);
        properties.put(
            AvailableSettings.GENERATE_STATISTICS,
            Boolean.TRUE.toString()
        );
        /*properties.put(
            AvailableSettings.CONNECTION_HANDLING,
            PhysicalConnectionHandlingMode.DELAYED_ACQUISITION_AND_RELEASE_AFTER_TRANSACTION
        );*/
    }
}
