package com.vladmihalcea.hpjp.util;

import com.vladmihalcea.hpjp.util.providers.DataSourceProvider;
import com.vladmihalcea.hpjp.util.providers.Database;
import org.assertj.core.util.Arrays;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * @author Vlad Mihalcea
 */
@RunWith(Parameterized.class)
public abstract class DataSourceProviderIntegrationTest extends AbstractTest {

    private final DataSourceProvider dataSourceProvider;

    public DataSourceProviderIntegrationTest(DataSourceProvider dataSourceProvider) {
        this.dataSourceProvider = dataSourceProvider;
    }

    @Parameterized.Parameters
    public static Collection<DataSourceProviderIntegrationTest[]> databSourceProviders() {
        List<DataSourceProviderIntegrationTest[]> databases = new ArrayList<>();
        return databases;
    }

    @Override
    protected Database database() {
        return dataSourceProvider.database();
    }

    @Override
    protected DataSourceProvider dataSourceProvider() {
        return dataSourceProvider;
    }
}
