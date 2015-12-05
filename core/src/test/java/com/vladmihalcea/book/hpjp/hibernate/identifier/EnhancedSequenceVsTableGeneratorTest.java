package com.vladmihalcea.book.hpjp.hibernate.identifier;

import java.util.Properties;

public class EnhancedSequenceVsTableGeneratorTest extends SequenceVsTableGeneratorTest {

    @Override
    protected Properties properties() {
        Properties properties = super.properties();
        properties.put("hibernate.id.new_generator_mappings", "true");
        return properties;
    }
}
