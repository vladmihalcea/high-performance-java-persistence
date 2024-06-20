package com.vladmihalcea.util;

import com.vladmihalcea.util.providers.Database;

/**
 * AbstractPostgreSQLIntegrationTest - Abstract PostgreSQL IntegrationTest
 *
 * @author Vlad Mihalcea
 */
public abstract class AbstractPostgreSQLIntegrationTest extends AbstractTest {

    @Override
    protected Database database() {
        return Database.POSTGRESQL;
    }
}
