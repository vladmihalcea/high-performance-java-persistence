package com.vladmihalcea.util;

import com.vladmihalcea.util.providers.Database;

/**
 * AbstractSQLServerIntegrationTest - Abstract SQL Server IntegrationTest
 *
 * @author Vlad Mihalcea
 */
public abstract class AbstractSQLServerIntegrationTest extends AbstractTest {

    @Override
    protected Database database() {
        return Database.SQLSERVER;
    }
}
