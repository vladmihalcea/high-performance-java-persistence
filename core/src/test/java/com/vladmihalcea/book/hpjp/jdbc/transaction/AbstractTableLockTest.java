package com.vladmihalcea.book.hpjp.jdbc.transaction;

import com.vladmihalcea.book.hpjp.util.AbstractTest;
import org.junit.Test;

import javax.persistence.*;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicReference;

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
        doInJDBC(connection -> {
            try (
                    PreparedStatement departmentStatement = connection.prepareStatement(INSERT_DEPARTMENT);
                    PreparedStatement employeeStatement = connection.prepareStatement(INSERT_EMPLOYEE);
            ) {
                int index = 0;
                departmentStatement.setString(++index, "Hypersistence");
                departmentStatement.setLong(++index, 100_000);
                departmentStatement.setLong(++index, 1);
                departmentStatement.executeUpdate();

                index = 0;
                departmentStatement.setString(++index, "Bitsystem");
                departmentStatement.setLong(++index, 10_000);
                departmentStatement.setLong(++index, 2);
                departmentStatement.executeUpdate();

                for (int i = 0; i < 3; i++) {
                    index = 0;
                    employeeStatement.setLong(++index, 1);
                    employeeStatement.setString(++index, String.format("John Doe %1$d", i));
                    employeeStatement.setLong(++index, 30_000);
                    employeeStatement.setLong(++index, i);
                    employeeStatement.executeUpdate();
                }
            } catch (SQLException e) {
                fail(e.getMessage());
            }
        });

    }

    @Test
    public void testPhantomReadAggregateWithTableLock() {
        AtomicReference<Boolean> carolPreventedByLocking = new AtomicReference<>();
        AtomicReference<Boolean> davePreventedByLocking = new AtomicReference<>();
        try {
            doInJDBC(aliceConnection -> {
                prepareConnection(aliceConnection);
                executeStatement(aliceConnection, lockEmployeeTableSql());

                /*long salaryCount = selectColumn(aliceConnection, sumEmployeeSalarySql(), Number.class).longValue();
                assertEquals(90_000, salaryCount);*/

                try {
                    LOGGER.debug("Add Carol on Department 1");
                    executeSync(() -> {
                        doInJDBC(bobConnection -> {
                            prepareConnection(bobConnection);
                            try (
                                    PreparedStatement employeeStatement = bobConnection.prepareStatement(insertEmployeeSql());
                            ) {
                                int employeeId = 4;
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
                        LOGGER.debug("Add Dave on Department 2");
                        doInJDBC(daveConnection -> {
                            prepareConnection(daveConnection);
                            try (
                                    PreparedStatement employeeStatement = daveConnection.prepareStatement(insertEmployeeSql());
                            ) {
                                int employeeId = 5;
                                int index = 0;
                                employeeStatement.setLong(++index, 2);
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
        doInJDBC(aliceConnection -> {
            long salaryCount = selectColumn(aliceConnection, sumEmployeeSalarySql(), Number.class).longValue();
            if(99_000 != salaryCount) {
                LOGGER.info("Table lock allows Phantom Read even when using Explicit Locks since the salary count is {} instead of 99000", salaryCount);
            }
            else {
                LOGGER.info("Table lock prevents Phantom Read when using Explicit Locks {}", carolPreventedByLocking.get() ? "due to locking" : "");
            }
        });
    }

    protected void prepareConnection(Connection connection) {
        try {
            connection.setTransactionIsolation(Connection.TRANSACTION_READ_COMMITTED);
            connection.setNetworkTimeout(Executors.newSingleThreadExecutor(), 3000);
        } catch (Throwable ignore) {
        }
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
    @Table(name = "employee")
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
