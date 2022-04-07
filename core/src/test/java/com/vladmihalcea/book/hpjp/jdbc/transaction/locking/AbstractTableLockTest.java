package com.vladmihalcea.book.hpjp.jdbc.transaction.locking;

import com.vladmihalcea.book.hpjp.util.AbstractTest;
import org.junit.Test;

import jakarta.persistence.*;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.junit.Assert.fail;

/**
 * @author Vlad Mihalcea
 */
public abstract class AbstractTableLockTest extends AbstractTest {

    public static final String INSERT_DEPARTMENT = "insert into department (name, budget, id) values (?, ?, ?)";

    public static final String INSERT_EMPLOYEE = "insert into employee (department_id, name, salary, id) values (?, ?, ?, ?)";

    @Override
    protected Class<?>[] entities() {
        return new Class[] {
            Department.class,
            Employee.class
        };
    }

    @Override
    public void init() {
        super.init();
        initData();
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

    @Test
    public void testPhantomReadAggregateWithTableLock() {
        AtomicBoolean carolPreventedByLocking = new AtomicBoolean();
        AtomicBoolean davePreventedByLocking = new AtomicBoolean();
        try {
            doInJDBC(aliceConnection -> {
                prepareConnection(aliceConnection);
                lockEmployeeTable(aliceConnection);

                try {
                    LOGGER.debug("Add Carol on Department 1");
                    executeSync(() -> {
                        doInJDBC(bobConnection -> {
                            prepareConnection(bobConnection);
                            try (
                                    PreparedStatement employeeStatement = bobConnection.prepareStatement(insertEmployeeSql());
                            ) {
                                int employeeId = 6;
                                int index = 0;
                                employeeStatement.setLong(++index, 1);
                                employeeStatement.setString(++index, "Carol");
                                employeeStatement.setLong(++index, 9_000);
                                employeeStatement.setLong(++index, employeeId);
                                employeeStatement.executeUpdate();
                            } catch (Exception e) {
                                LOGGER.info("Exception thrown", e);
                                carolPreventedByLocking.set(true);
                            }
                        });
                    });
                } catch (Exception e) {
                    LOGGER.info("Exception thrown", e);
                    carolPreventedByLocking.set(true);
                }

                try {
                    executeSync(() -> {
                        LOGGER.debug("Add Dave on Department 3");
                        doInJDBC(daveConnection -> {
                            prepareConnection(daveConnection);
                            try (
                                    PreparedStatement employeeStatement = daveConnection.prepareStatement(insertEmployeeSql());
                            ) {
                                int employeeId = 7;
                                int index = 0;
                                employeeStatement.setLong(++index, 3);
                                employeeStatement.setString(++index, "Dave");
                                employeeStatement.setLong(++index, 9_000);
                                employeeStatement.setLong(++index, employeeId);
                                employeeStatement.executeUpdate();
                            } catch (Exception e) {
                                LOGGER.info("Exception thrown", e);
                                davePreventedByLocking.set(true);
                            }
                        });
                    });
                } catch (Exception e) {
                    LOGGER.info("Exception thrown", e);
                    davePreventedByLocking.set(true);
                }
                update(aliceConnection, updateEmployeeSalarySql());
            });
        } catch (Exception e) {
            LOGGER.info("Exception thrown", e);
            carolPreventedByLocking.set(true);
        }
        LOGGER.info("Carol insert was {} prevented by lock", carolPreventedByLocking.get() ? "" : "not");
        LOGGER.info("Dave insert was {} prevented by lock", davePreventedByLocking.get() ? "" : "not");
    }

    protected void prepareConnection(Connection connection) {
        try {
            connection.setTransactionIsolation(Connection.TRANSACTION_READ_COMMITTED);
            connection.setNetworkTimeout(Executors.newSingleThreadExecutor(), 3000);
        } catch (Throwable ignore) {
        }
    }

    protected void lockEmployeeTable(Connection connection) {
        executeStatement(connection, lockEmployeeTableSql());
    }

    protected abstract String lockEmployeeTableSql();

    protected String sumEmployeeSalarySql() {
        return "SELECT SUM(salary) FROM employee where department_id = 1";
    }

    protected String insertEmployeeSql() {
        return INSERT_EMPLOYEE;
    }

    protected String updateEmployeeSalarySql() {
        return "UPDATE employee SET salary = salary * 1.1 WHERE department_id = 1";
    }

    @Entity(name = "Department")
    @Table(name = "department")
    public static class Department {

        @Id
        private Long id;

        private String name;

        private long budget;

        public Long getId() {
            return id;
        }

        public void setId(Long id) {
            this.id = id;
        }

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public long getBudget() {
            return budget;
        }

        public void setBudget(long budget) {
            this.budget = budget;
        }
    }

    @Entity(name = "Employee")
    @Table(name = "employee", indexes = @Index(name = "IDX_DEPARTMENT_ID", columnList = "department_id"))
    public static class Employee {

        @Id
        private Long id;

        @Column(name = "name")
        private String name;

        @ManyToOne(fetch = FetchType.LAZY)
        private Department department;

        private long salary;

        public Long getId() {
            return id;
        }

        public void setId(Long id) {
            this.id = id;
        }

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public Department getDepartment() {
            return department;
        }

        public void setDepartment(Department department) {
            this.department = department;
        }

        public long getSalary() {
            return salary;
        }

        public void setSalary(long salary) {
            this.salary = salary;
        }
    }
}
