package com.vladmihalcea.book.hpjp.jdbc.transaction.locking;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;

import com.vladmihalcea.book.hpjp.jdbc.transaction.locking.AbstractTableLockTest;
import com.vladmihalcea.book.hpjp.util.providers.DataSourceProvider;
import com.vladmihalcea.book.hpjp.util.providers.MySQLDataSourceProvider;

import static org.junit.Assert.fail;

/**
 * @author Vlad Mihalcea
 */
public class MySQLTableLockWithEmployeesInFirstDepartmentOnlyTest extends AbstractTableLockTest {

    @Override
    public void initData() {
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

                index = 0;
                employeeStatement.setLong(++index, 1L);
                employeeStatement.setString(++index, "CEO");
                employeeStatement.setLong(++index, 30_000);
                employeeStatement.setLong(++index, 1L);
                employeeStatement.executeUpdate();

                index = 0;
                employeeStatement.setLong(++index, 1L);
                employeeStatement.setString(++index, "CTO");
                employeeStatement.setLong(++index, 30_000);
                employeeStatement.setLong(++index, 2L);
                employeeStatement.executeUpdate();

            } catch (SQLException e) {
                fail(e.getMessage());
            }
        });

    }

    @Override
    protected DataSourceProvider dataSourceProvider() {
        return new MySQLDataSourceProvider();
    }

    @Override
    protected String lockEmployeeTableSql() {
        return "SELECT * FROM employee WHERE department_id = 1 FOR UPDATE";
    }

    @Override
    protected void prepareConnection(Connection connection) {
        /*try {
            connection.setTransactionIsolation(Connection.TRANSACTION_READ_COMMITTED);
        } catch (SQLException e) {
            fail(e.getMessage());
        }*/
        executeStatement(connection, "SET GLOBAL innodb_lock_wait_timeout = 1");
    }
}
