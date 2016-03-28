package com.vladmihalcea.book.hpjp.hibernate.identifier.batch;

import com.vladmihalcea.book.hpjp.util.AbstractTest;

import java.util.Properties;

public abstract class AbstractBatchIdentifierTest extends AbstractTest {

    @Override
    protected Properties properties() {
        Properties properties = super.properties();
        properties.put("hibernate.order_inserts", "true");
        properties.put("hibernate.order_updates", "true");
        properties.put("hibernate.jdbc.batch_size", "2");
        return properties;
    }

}
