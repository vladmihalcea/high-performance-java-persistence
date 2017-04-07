package com.vladmihalcea.book.hpjp.util;

import com.vladmihalcea.book.hpjp.util.providers.CockroachDBDataSourceProvider;
import com.vladmihalcea.book.hpjp.util.providers.DataSourceProvider;
import com.vladmihalcea.book.hpjp.util.providers.PostgreSQLDataSourceProvider;

/**
 * AbstractCockroachDBIntegrationTest - Abstract CockroachDB IntegrationTest
 *
 * @author Vlad Mihalcea
 */
public abstract class AbstractCockroachDBIntegrationTest extends AbstractTest {

    @Override
    protected DataSourceProvider dataSourceProvider() {
        return new CockroachDBDataSourceProvider();
    }
}
