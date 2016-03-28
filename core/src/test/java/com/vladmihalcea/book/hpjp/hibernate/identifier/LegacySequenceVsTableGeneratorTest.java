package com.vladmihalcea.book.hpjp.hibernate.identifier;

import java.util.Properties;

public class LegacySequenceVsTableGeneratorTest extends SequenceVsTableGeneratorTest {

    @Override
    protected Properties properties() {
        Properties properties = super.properties();
        properties.put("hibernate.id.new_generator_mappings", "false");
        return properties;
    }
}
