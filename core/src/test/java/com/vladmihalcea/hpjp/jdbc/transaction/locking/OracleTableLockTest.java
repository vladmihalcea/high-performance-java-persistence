package com.vladmihalcea.hpjp.jdbc.transaction.locking;

import com.vladmihalcea.hpjp.util.providers.DataSourceProvider;
import com.vladmihalcea.hpjp.util.providers.OracleDataSourceProvider;

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
        return "LOCK TABLE employee IN SHARE MODE";
    }
}
