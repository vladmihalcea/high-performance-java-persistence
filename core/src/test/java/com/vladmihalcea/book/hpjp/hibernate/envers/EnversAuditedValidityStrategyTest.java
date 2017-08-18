package com.vladmihalcea.book.hpjp.hibernate.envers;

import java.util.Properties;

import org.hibernate.envers.configuration.EnversSettings;
import org.hibernate.envers.strategy.ValidityAuditStrategy;

/**
 * @author Vlad Mihalcea
 */
public class EnversAuditedValidityStrategyTest extends EnversAuditedDefaultStrategyTest {

    @Override
    protected Properties properties() {
        Properties properties = super.properties();
        properties.setProperty(
            EnversSettings.AUDIT_STRATEGY,
            ValidityAuditStrategy.class.getName()
        );
        return properties;
    }
}
