package com.vladmihalcea.hpjp.util;

import com.vladmihalcea.hpjp.util.providers.Database;

/**
 * AbstractCockroachDBIntegrationTest - Abstract CockroachDB IntegrationTest
 *
 * @author Vlad Mihalcea
 */
public abstract class AbstractCockroachDBIntegrationTest extends AbstractTest {

    @Override
    protected Database database() {
        return Database.COCKROACHDB;
    }
}
