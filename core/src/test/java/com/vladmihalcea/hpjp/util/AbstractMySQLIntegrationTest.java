package com.vladmihalcea.hpjp.util;

import com.vladmihalcea.hpjp.util.providers.Database;

/**
 * AbstractMySQLIntegrationTest - Abstract MySQL IntegrationTest
 *
 * @author Vlad Mihalcea
 */
public abstract class AbstractMySQLIntegrationTest extends AbstractTest {

    @Override
    protected Database database() {
        return Database.MYSQL;
    }
}
