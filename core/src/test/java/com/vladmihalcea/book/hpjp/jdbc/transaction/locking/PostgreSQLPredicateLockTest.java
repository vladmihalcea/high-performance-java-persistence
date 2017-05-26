package com.vladmihalcea.book.hpjp.jdbc.transaction.locking;

import com.vladmihalcea.book.hpjp.jdbc.transaction.locking.AbstractPredicateLockTest;
import com.vladmihalcea.book.hpjp.util.providers.DataSourceProvider;
import com.vladmihalcea.book.hpjp.util.providers.PostgreSQLDataSourceProvider;

/**
 * @author Vlad Mihalcea
 */
public class PostgreSQLPredicateLockTest extends AbstractPredicateLockTest {

    @Override
    protected DataSourceProvider dataSourceProvider() {
        return new PostgreSQLDataSourceProvider();
    }
}
