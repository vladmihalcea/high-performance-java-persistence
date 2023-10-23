package com.vladmihalcea.hpjp.jdbc.transaction.phenomena;

import com.vladmihalcea.hpjp.util.providers.DataSourceProvider;
import com.vladmihalcea.hpjp.util.providers.YugabyteDBDataSourceProvider;
import org.junit.runners.Parameterized;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

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

    //Ignore test
    @Parameterized.Parameters
    public static Collection<Object[]> isolationLevels() {
        List<Object[]> levels = new ArrayList<>();
        return levels;
    }
}
