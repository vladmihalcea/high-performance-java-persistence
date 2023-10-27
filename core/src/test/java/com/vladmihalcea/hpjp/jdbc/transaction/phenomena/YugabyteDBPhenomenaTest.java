package com.vladmihalcea.hpjp.jdbc.transaction.phenomena;

import com.vladmihalcea.hpjp.util.providers.DataSourceProvider;
import com.vladmihalcea.hpjp.util.providers.YugabyteDBDataSourceProvider;
import org.junit.runners.Parameterized;

import java.util.Collection;
import java.util.Collections;

/**
 * YugabyteDBPhenomenaTest - Test to validate YugabyteDB phenomena
 *
 * @author Vlad Mihalcea
 */
public class YugabyteDBPhenomenaTest extends AbstractPhenomenaTest {

    public YugabyteDBPhenomenaTest(String isolationLevelName, int isolationLevel) {
        super(isolationLevelName, isolationLevel);
    }

    @Override
    protected DataSourceProvider dataSourceProvider() {
        return new YugabyteDBDataSourceProvider();
    }

    @Parameterized.Parameters
    public static Collection<Object[]> isolationLevels() {
        if(!ENABLE_LONG_RUNNING_TESTS) {
            return Collections.emptyList();
        }
        return AbstractPhenomenaTest.isolationLevels();
    }
}
