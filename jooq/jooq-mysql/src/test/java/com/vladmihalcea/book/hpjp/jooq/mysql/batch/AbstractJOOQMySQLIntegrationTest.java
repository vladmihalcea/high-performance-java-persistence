package com.vladmihalcea.book.hpjp.jooq.mysql.batch;

import com.vladmihalcea.book.hpjp.jooq.AbstractJOOQIntegrationTest;
import org.jooq.SQLDialect;

/**
 * @author Vlad Mihalcea
 */
public abstract class AbstractJOOQMySQLIntegrationTest extends AbstractJOOQIntegrationTest {

    @Override
    protected String ddlFolder() {
        return "mysql";
    }

    @Override
    protected SQLDialect sqlDialect() {
        return SQLDialect.MYSQL;
    }

    protected DataSourceProvider dataSourceProvider() {
        return new MySQLDataSourceProvider();
    }
}
