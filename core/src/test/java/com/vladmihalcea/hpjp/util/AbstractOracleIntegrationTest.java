package com.vladmihalcea.hpjp.util;

import com.vladmihalcea.hpjp.util.providers.Database;

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
