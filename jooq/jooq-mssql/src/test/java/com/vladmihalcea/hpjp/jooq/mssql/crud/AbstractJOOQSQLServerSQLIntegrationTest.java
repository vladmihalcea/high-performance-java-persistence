package com.vladmihalcea.hpjp.jooq.mssql.crud;

import com.vladmihalcea.hpjp.jooq.AbstractJOOQIntegrationTest;
import com.vladmihalcea.hpjp.util.providers.DataSourceProvider;
import com.vladmihalcea.hpjp.util.providers.SQLServerDataSourceProvider;
import org.jooq.SQLDialect;

/**
 * @author Vlad Mihalcea
 */
public abstract class AbstractJOOQSQLServerSQLIntegrationTest extends AbstractJOOQIntegrationTest {

    @Override
    protected String ddlFolder() {
        return "mssql";
    }

    @Override
    protected SQLDialect sqlDialect() {
        return SQLDialect.SQLSERVER2014;
    }

    protected DataSourceProvider dataSourceProvider() {
        return new SQLServerDataSourceProvider();
    }
}
