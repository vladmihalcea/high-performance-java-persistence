package com.vladmihalcea.book.hpjp.jdbc.transaction.locking;

import com.vladmihalcea.book.hpjp.jdbc.transaction.locking.AbstractTableLockTest;
import com.vladmihalcea.book.hpjp.util.providers.DataSourceProvider;
import com.vladmihalcea.book.hpjp.util.providers.OracleDataSourceProvider;

/**
 * @author Vlad Mihalcea
 */
public class OracleTableLockTest extends AbstractTableLockTest {

    @Override
    protected DataSourceProvider dataSourceProvider() {
        return new OracleDataSourceProvider();
    }

    @Override
    protected String lockEmployeeTableSql() {
        return "LOCK TABLE employee IN SHARE MODE NOWAIT";
    }
}
