package com.vladmihalcea.book.hpjp.jdbc.transaction;

/**
 * @author Vlad Mihalcea
 */
public class PostgreSQLPredicateLockTest extends AbstractPredicateLockTest {

    @Override
    protected DataSourceProvider dataSourceProvider() {
        return new PostgreSQLDataSourceProvider();
    }
}
