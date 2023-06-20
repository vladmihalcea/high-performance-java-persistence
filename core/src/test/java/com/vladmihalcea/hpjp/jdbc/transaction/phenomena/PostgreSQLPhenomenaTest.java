package com.vladmihalcea.hpjp.jdbc.transaction.phenomena;

import com.vladmihalcea.hpjp.util.providers.DataSourceProvider;
import com.vladmihalcea.hpjp.util.providers.PostgreSQLDataSourceProvider;

/**
 * PostgreSQLPhenomenaTest - Test to validate PostgreSQL phenomena
 *
 * @author Vlad Mihalcea
 */
public class PostgreSQLPhenomenaTest extends AbstractPhenomenaTest {

    public PostgreSQLPhenomenaTest(String isolationLevelName, int isolationLevel) {
        super(isolationLevelName, isolationLevel);
    }

    @Override
    protected DataSourceProvider dataSourceProvider() {
        return new PostgreSQLDataSourceProvider();
    }
}
