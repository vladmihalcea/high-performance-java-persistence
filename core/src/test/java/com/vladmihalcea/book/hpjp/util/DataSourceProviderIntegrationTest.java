package com.vladmihalcea.book.hpjp.util;

import com.vladmihalcea.book.hpjp.util.providers.*;
import org.assertj.core.util.Arrays;
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

    public DataSourceProviderIntegrationTest(Database database) {
        this.dataSourceProvider = database.dataSourceProvider();
    }

    @Parameterized.Parameters
    public static Collection<Database[]> databases() {
        List<Database[]> databases = new ArrayList<>();
        databases.add(Arrays.array(Database.ORACLE));
        databases.add(Arrays.array(Database.SQLSERVER));
        databases.add(Arrays.array(Database.POSTGRESQL));
        databases.add(Arrays.array(Database.MYSQL));
        return databases;
    }

    @Override
    protected DataSourceProvider dataSourceProvider() {
        return dataSourceProvider;
    }
}
