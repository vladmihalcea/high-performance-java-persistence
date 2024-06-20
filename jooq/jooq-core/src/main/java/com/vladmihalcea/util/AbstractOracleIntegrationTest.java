package com.vladmihalcea.util;

import com.vladmihalcea.util.providers.Database;

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
