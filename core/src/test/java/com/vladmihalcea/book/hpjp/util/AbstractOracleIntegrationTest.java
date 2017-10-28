package com.vladmihalcea.book.hpjp.util;

import com.vladmihalcea.book.hpjp.util.providers.DataSourceProvider;
import com.vladmihalcea.book.hpjp.util.providers.Database;
import com.vladmihalcea.book.hpjp.util.providers.OracleDataSourceProvider;

/**
 * AbstractOracleXEIntegrationTest - Abstract Orcale XE IntegrationTest
 *
 * @author Vlad Mihalcea
 */
public abstract class AbstractOracleIntegrationTest extends AbstractTest {

    @Override
    protected Database database() {
        return Database.ORACLE;
    }
}
