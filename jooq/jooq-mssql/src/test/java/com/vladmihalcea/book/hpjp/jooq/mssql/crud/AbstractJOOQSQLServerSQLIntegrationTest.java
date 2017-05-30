package com.vladmihalcea.book.hpjp.jooq.mssql.crud;

import com.vladmihalcea.book.hpjp.jooq.AbstractJOOQIntegrationTest;
import com.vladmihalcea.book.hpjp.util.providers.DataSourceProvider;
import com.vladmihalcea.book.hpjp.util.providers.SQLServerDataSourceProvider;
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
