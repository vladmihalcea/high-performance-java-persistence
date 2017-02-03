package com.vladmihalcea.book.hpjp.jdbc.transaction;

/**
 * @author Vlad Mihalcea
 */
public class SQLServerPredicateLockTest extends AbstractPredicateLockTest {

    @Override
    protected DataSourceProvider dataSourceProvider() {
        return new SQLServerDataSourceProvider();
    }
}
