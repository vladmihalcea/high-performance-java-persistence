package com.vladmihalcea.hpjp.hibernate.cache;

import java.util.Properties;


/**
 *
 * @author Vlad Mihalcea
 */
public class LoadedStateReferenceEntitiesTest extends LoadedStateBenchmarkTest {

    public LoadedStateReferenceEntitiesTest(int insertCount) {
        super(insertCount);
    }

    @Override
    protected Properties properties() {
        Properties properties = super.properties();
        properties.put("hibernate.cache.use_reference_entries", Boolean.TRUE.toString());
        return properties;
    }

}
