package com.vladmihalcea.book.hpjp.jdbc.transaction;

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
