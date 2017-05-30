package com.vladmihalcea.book.hpjp.jooq.pgsql.score;

import com.vladmihalcea.book.hpjp.jooq.AbstractJOOQIntegrationTest;
import com.vladmihalcea.book.hpjp.util.providers.DataSourceProvider;
import com.vladmihalcea.book.hpjp.util.providers.PostgreSQLDataSourceProvider;
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
        return SQLDialect.POSTGRES_9_5;
    }

    protected DataSourceProvider dataSourceProvider() {
        return new PostgreSQLDataSourceProvider();
    }
}
