package com.vladmihalcea.book.hpjp.jdbc.transaction;

/**
 * @author Vlad Mihalcea
 */
public class OraclePredicateLockTest extends AbstractPredicateLockTest {

    @Override
    protected DataSourceProvider dataSourceProvider() {
        return new OracleDataSourceProvider();
    }
}
