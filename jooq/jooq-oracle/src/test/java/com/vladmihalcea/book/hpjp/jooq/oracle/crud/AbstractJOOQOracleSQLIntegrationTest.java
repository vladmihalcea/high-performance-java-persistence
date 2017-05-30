package com.vladmihalcea.book.hpjp.jooq.oracle.crud;

import com.vladmihalcea.book.hpjp.jooq.AbstractJOOQIntegrationTest;
import com.vladmihalcea.book.hpjp.util.providers.DataSourceProvider;
import com.vladmihalcea.book.hpjp.util.providers.OracleDataSourceProvider;
import org.jooq.SQLDialect;

/**
 * @author Vlad Mihalcea
 */
public abstract class AbstractJOOQOracleSQLIntegrationTest extends AbstractJOOQIntegrationTest {

    @Override
    protected String ddlFolder() {
        return "oracle";
    }

    @Override
    protected SQLDialect sqlDialect() {
        return SQLDialect.ORACLE11G;
    }

    protected DataSourceProvider dataSourceProvider() {
        return new OracleDataSourceProvider();
    }
}
