package com.vladmihalcea.book.hpjp.hibernate.criteria.literal;

import org.hibernate.cfg.AvailableSettings;
import org.hibernate.query.criteria.LiteralHandlingMode;

import java.util.Properties;

/**
 * @author Vlad Mihalcea
 */
public class BindCriteriaLiteralTest extends DefaultCriteriaLiteralTest {

    @Override
    protected void additionalProperties(Properties properties) {
        properties.put(AvailableSettings.CRITERIA_LITERAL_HANDLING_MODE, LiteralHandlingMode.BIND);
    }
}
