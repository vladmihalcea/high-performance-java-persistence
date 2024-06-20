package com.vladmihalcea.hpjp.jooq.pgsql.score;

import com.vladmihalcea.hpjp.jooq.AbstractJOOQIntegrationTest;
import com.vladmihalcea.util.providers.DataSourceProvider;
import com.vladmihalcea.util.providers.PostgreSQLDataSourceProvider;
import org.jooq.SQLDialect;

/**
 * @author Vlad Mihalcea
 */
public abstract class AbstractJOOQPostgreSQLIntegrationTest extends AbstractJOOQIntegrationTest {

    @Override
    protected String ddlFolder() {
        return "pgsql";
    }

    @Override
    protected SQLDialect sqlDialect() {
        return SQLDialect.POSTGRES;
    }

    protected DataSourceProvider dataSourceProvider() {
        return new PostgreSQLDataSourceProvider();
    }
}
