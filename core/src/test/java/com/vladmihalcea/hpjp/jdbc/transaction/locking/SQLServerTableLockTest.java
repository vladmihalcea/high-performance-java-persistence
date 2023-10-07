package com.vladmihalcea.hpjp.jdbc.transaction.locking;

import com.vladmihalcea.hpjp.util.providers.DataSourceProvider;
import com.vladmihalcea.hpjp.util.providers.SQLServerDataSourceProvider;

/**
 * @author Vlad Mihalcea
 */
public class SQLServerTableLockTest extends AbstractTableLockTest {

    @Override
    protected DataSourceProvider dataSourceProvider() {
        return new SQLServerDataSourceProvider();
    }

    @Override
    protected String lockEmployeeTableSql() {
        return "SELECT 1 FROM employee WITH (HOLDLOCK) WHERE department_id = 1";
    }

    protected String insertEmployeeSql() {
        return "INSERT INTO employee WITH(NOWAIT) (department_id, name, salary, id) VALUES (?, ?, ?, ?)";
    }
}
