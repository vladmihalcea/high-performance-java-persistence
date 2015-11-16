package com.vladmihalcea.book.hpjp.util;

import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * DataSourceProviderIntegrationTest - Test against some common RDBMS providers
 *
 * @author Vlad Mihalcea
 */
@RunWith(Parameterized.class)
public abstract class DataSourceProviderIntegrationTest extends AbstractTest {

    private final DataSourceProvider dataSourceProvider;

    public DataSourceProviderIntegrationTest(DataSourceProvider dataSourceProvider) {
        this.dataSourceProvider = dataSourceProvider;
    }

    @Parameterized.Parameters
    public static Collection<DataSourceProvider[]> rdbmsDataSourceProvider() {
        List<DataSourceProvider[]> providers = new ArrayList<>();
        providers.add(new DataSourceProvider[]{new OracleDataSourceProvider()});
        providers.add(new DataSourceProvider[]{new SQLServerDataSourceProvider()});
        providers.add(new DataSourceProvider[]{new PostgreSQLDataSourceProvider()});
        providers.add(new DataSourceProvider[]{new MySQLDataSourceProvider()});
        return providers;
    }

    @Override
    protected DataSourceProvider getDataSourceProvider() {
        return dataSourceProvider;
    }
}
