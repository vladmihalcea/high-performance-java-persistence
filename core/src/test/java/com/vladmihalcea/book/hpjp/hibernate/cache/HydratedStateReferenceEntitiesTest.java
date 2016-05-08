package com.vladmihalcea.book.hpjp.hibernate.cache;

import java.util.Properties;


/**
 * ReadOnlyCacheConcurrencyStrategyReferenceEntitiesTest - Test to check CacheConcurrencyStrategy.READ_ONLY
 *     with hibernate.cache.use_reference_entries doesn't work because Commit has a collection of CommitChanges
 *
 * @author Vlad Mihalcea
 */
public class HydratedStateReferenceEntitiesTest extends HydratedStateBenchmarkTest {

    public HydratedStateReferenceEntitiesTest(int insertCount) {
        super(insertCount);
    }

    @Override
    protected Properties properties() {
        Properties properties = super.properties();
        properties.put("hibernate.cache.use_reference_entries", Boolean.TRUE.toString());
        return properties;
    }

}
