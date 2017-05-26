package com.vladmihalcea.book.hpjp.jdbc.transaction.locking;

import java.sql.PreparedStatement;
import java.sql.SQLException;

import com.vladmihalcea.book.hpjp.jdbc.transaction.locking.AbstractTableLockTest;
import com.vladmihalcea.book.hpjp.util.providers.DataSourceProvider;
import com.vladmihalcea.book.hpjp.util.providers.SQLServerDataSourceProvider;

import static org.junit.Assert.fail;

/**
 * @author Vlad Mihalcea
 */
public class SQLServerTableLockMultipleEntriesTest extends AbstractTableLockTest {

    @Override
    protected DataSourceProvider dataSourceProvider() {
        return new SQLServerDataSourceProvider();
    }

    @Override
    protected String lockEmployeeTableSql() {
        return "SELECT * FROM employee WITH (HOLDLOCK) WHERE department_id = 1";
    }

    protected String insertEmployeeSql() {
        return "INSERT INTO employee WITH(NOWAIT) (department_id, name, salary, id) VALUES (?, ?, ?, ?)";
    }

    protected void initData() {
        doInJDBC(connection -> {
            try (
                    PreparedStatement departmentStatement = connection.prepareStatement(INSERT_DEPARTMENT);
                    PreparedStatement employeeStatement = connection.prepareStatement(INSERT_EMPLOYEE);
            ) {
                int index = 0;
                departmentStatement.setString(++index, "Department 1");
                departmentStatement.setLong(++index, 100_000);
                departmentStatement.setLong(++index, 1);
                departmentStatement.executeUpdate();

                index = 0;
                departmentStatement.setString(++index, "Department 2");
                departmentStatement.setLong(++index, 75_000);
                departmentStatement.setLong(++index, 2);
                departmentStatement.executeUpdate();

                index = 0;
                departmentStatement.setString(++index, "Department 3");
                departmentStatement.setLong(++index,90_000);
                departmentStatement.setLong(++index, 3);
                departmentStatement.executeUpdate();

                for (long i = 100; i < 1000; i++) {
                    index = 0;
                    employeeStatement.setLong(++index, 1L);
                    employeeStatement.setString(++index, "CEO");
                    employeeStatement.setLong(++index, 30_000);
                    employeeStatement.setLong(++index, i);
                    employeeStatement.executeUpdate();
                }

                for (long i = 1001; i < 2000; i++) {
                    index = 0;
                    employeeStatement.setLong(++index, 2L);
                    employeeStatement.setString(++index, "CTO");
                    employeeStatement.setLong(++index, 30_000);
                    employeeStatement.setLong(++index, i);
                    employeeStatement.executeUpdate();
                }

                index = 0;
                employeeStatement.setLong(++index, 2L);
                employeeStatement.setString(++index, "CEO");
                employeeStatement.setLong(++index, 30_000);
                employeeStatement.setLong(++index, 3L);
                employeeStatement.executeUpdate();

            } catch (SQLException e) {
                fail(e.getMessage());
            }
        });
    }
}
