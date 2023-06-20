package com.vladmihalcea.hpjp.hibernate.audit.envers;

import java.util.Properties;

import org.hibernate.envers.configuration.EnversSettings;
import org.hibernate.envers.strategy.internal.ValidityAuditStrategy;

/**
 * @author Vlad Mihalcea
 */
public class EnversAuditedValidityStrategyTest extends EnversAuditedDefaultStrategyTest {

    @Override
    protected void additionalProperties(Properties properties) {
        properties.setProperty(
            EnversSettings.AUDIT_STRATEGY,
            ValidityAuditStrategy.class.getName()
        );
    }
}
