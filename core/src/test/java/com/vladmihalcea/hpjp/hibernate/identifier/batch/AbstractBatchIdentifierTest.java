package com.vladmihalcea.hpjp.hibernate.identifier.batch;

import com.vladmihalcea.hpjp.util.AbstractTest;

import java.util.Properties;

public abstract class AbstractBatchIdentifierTest extends AbstractTest {

    @Override
    protected void additionalProperties(Properties properties) {
        properties.put("hibernate.order_inserts", "true");
        properties.put("hibernate.order_updates", "true");
        properties.put("hibernate.jdbc.batch_size", "2");
    }

}
