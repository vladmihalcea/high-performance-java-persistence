package com.vladmihalcea.book.hpjp.jdbc.transaction.phenomena.linearizabilty;

import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.Id;
import javax.persistence.Index;
import javax.persistence.ManyToOne;
import javax.persistence.Table;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import com.vladmihalcea.book.hpjp.jdbc.transaction.phenomena.AbstractPhenomenaTest;
import com.vladmihalcea.book.hpjp.util.exception.ExceptionUtil;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

/**
 * PhenomenaTest - Test to validate what phenomena does a certain isolation level prevents
 *
 * @author Vlad Mihalcea
 */
@RunWith(Parameterized.class)
public abstract class AbstractLinearizabilityPhenomenaTest extends AbstractPhenomenaTest {

    protected AbstractLinearizabilityPhenomenaTest(String isolationLevelName, int isolationLevel) {
        super(isolationLevelName, isolationLevel);
    }

    @Override
    protected Class<?>[] entities() {
        List<Class<?>> classes = new ArrayList<>(Arrays.asList(super.entities()));
        classes.add(Department.class);
        classes.add(Employee.class);
        return classes.toArray(new Class<?>[]{});
    }

    protected String sumEmployeeSalarySql() {
        return "SELECT SUM(salary) FROM employee where department_id = 1";
    }

    protected String allEmployeeSalarySql() {
        return "SELECT salary FROM employee where department_id = 1";
    }

    protected String insertEmployeeSql() {
        return INSERT_EMPLOYEE;
    }

    protected String updateEmployeeSalarySql() {
        return "UPDATE employee SET salary = salary * 1.1 WHERE department_id = 1";
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
    public void testPhantomReadAggregate() {
        final AtomicBoolean preventedByLocking = new AtomicBoolean();
        final AtomicBoolean preventedByMVCC = new AtomicBoolean();

        try {
            doInJDBC(aliceConnection -> {
                if (!aliceConnection.getMetaData().supportsTransactionIsolationLevel(isolationLevel)) {
                    LOGGER.info("Database {} doesn't support {}", dataSourceProvider().database(), isolationLevelName);
                    return;
                }
                prepareConnection(aliceConnection);
                long salaryCount = selectColumn(aliceConnection, sumEmployeeSalarySql(), Number.class).longValue();
                assertEquals(90_000, salaryCount);

                try {
                    executeSync(() -> {
                        doInJDBC(bobConnection -> {
                            prepareConnection(bobConnection);
                            try {
                                long _salaryCount = selectColumn(bobConnection, sumEmployeeSalarySql(), Number.class).longValue();
                                assertEquals(90_000, _salaryCount);

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
                                }
                            } catch (Exception e) {
                                if( ExceptionUtil.isLockTimeout( e )) {
                                    preventedByLocking.set( true );
                                } else if( ExceptionUtil.isMVCCAnomalyDetection( e )) {
                                    preventedByMVCC.set( true );
                                } else {
                                    throw new IllegalStateException( e );
                                }
                            }
                        });
                    });
                } catch (Exception e) {
                    if( ExceptionUtil.isLockTimeout( e )) {
                        preventedByLocking.set( true );
                    } else if( ExceptionUtil.isMVCCAnomalyDetection( e )) {
                        preventedByMVCC.set( true );
                    } else {
                        throw new IllegalStateException( e );
                    }
                }
                update(aliceConnection, "UPDATE employee SET salary = salary * 1.1 WHERE department_id = 1");
            });
        } catch (Exception e) {
            if( ExceptionUtil.isLockTimeout( e )) {
                preventedByLocking.set( true );
            } else if( ExceptionUtil.isMVCCAnomalyDetection( e )) {
                preventedByMVCC.set( true );
            } else {
                throw new IllegalStateException( e );
            }
        }
        doInJDBC(aliceConnection -> {
            long salaryCount = selectColumn(aliceConnection, sumEmployeeSalarySql(), Number.class).longValue();
            if(99_000 != salaryCount) {
                LOGGER.info("Isolation level {} allows Phantom Read since the salary count is {} instead of 99000", isolationLevelName, salaryCount);
            }
            else {
                LOGGER.info("Isolation level {} prevents Phantom Read due to {}", isolationLevelName, preventedByLocking.get() ? "locking" : preventedByMVCC.get() ? "MVCC" : "unknown");
            }
        });
    }

    @Test
    public void testPhantomReadAggregateWithInsert() {
        final AtomicBoolean preventedByLocking = new AtomicBoolean();
        final AtomicBoolean preventedByMVCC = new AtomicBoolean();

        try {
            doInJDBC(aliceConnection -> {
                if (!aliceConnection.getMetaData().supportsTransactionIsolationLevel(isolationLevel)) {
                    LOGGER.info("Database {} doesn't support {}", dataSourceProvider().database(), isolationLevelName);
                    return;
                }
                prepareConnection(aliceConnection);
                long salaryCount = selectColumn(aliceConnection, sumEmployeeSalarySql(), Number.class).longValue();
                assertEquals(90_000, salaryCount);

                try {
                    executeSync(() -> {
                        doInJDBC(bobConnection -> {
                            prepareConnection(bobConnection);
                            try {
                                long _salaryCount = selectColumn(bobConnection, sumEmployeeSalarySql(), Number.class).longValue();
                                assertEquals(90_000, _salaryCount);

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
                                }
                            } catch (Exception e) {
                                if( ExceptionUtil.isLockTimeout( e )) {
                                    preventedByLocking.set( true );
                                } else if( ExceptionUtil.isMVCCAnomalyDetection( e )) {
                                    preventedByMVCC.set( true );
                                } else {
                                    throw new IllegalStateException( e );
                                }
                            }
                        });
                    });
                } catch (Exception e) {
                    if( ExceptionUtil.isLockTimeout( e )) {
                        preventedByLocking.set( true );
                    } else if( ExceptionUtil.isMVCCAnomalyDetection( e )) {
                        preventedByMVCC.set( true );
                    } else {
                        throw new IllegalStateException( e );
                    }
                }
                try (
                        PreparedStatement employeeStatement = aliceConnection.prepareStatement(insertEmployeeSql());
                ) {
                    int employeeId = 5;
                    int index = 0;
                    employeeStatement.setLong(++index, 1);
                    employeeStatement.setString(++index, "Dave");
                    employeeStatement.setLong(++index, 9_000);
                    employeeStatement.setLong(++index, employeeId);
                    employeeStatement.executeUpdate();
                }
            });
        } catch (Exception e) {
            if( ExceptionUtil.isLockTimeout( e )) {
                preventedByLocking.set( true );
            } else if( ExceptionUtil.isMVCCAnomalyDetection( e )) {
                preventedByMVCC.set( true );
            } else {
                throw new IllegalStateException( e );
            }
        }
        doInJDBC(aliceConnection -> {
            long salaryCount = selectColumn(aliceConnection, sumEmployeeSalarySql(), Number.class).longValue();
            if(99_000 != salaryCount) {
                LOGGER.info("Isolation level {} allows Phantom Read since the salary count is {} instead of 99000", isolationLevelName, salaryCount);
            }
            else {
                LOGGER.info("Isolation level {} prevents Phantom Read due to {}", isolationLevelName, preventedByLocking.get() ? "locking" : preventedByMVCC.get() ? "MVCC" : "unknown");
            }
        });
    }

    @Test
    public void testPhantomWriteSelectColumn() {
        final AtomicBoolean preventedByLocking = new AtomicBoolean();
        final AtomicBoolean preventedByMVCC = new AtomicBoolean();

        try {
            doInJDBC(aliceConnection -> {
                if (!aliceConnection.getMetaData().supportsTransactionIsolationLevel(isolationLevel)) {
                    LOGGER.info("Database {} doesn't support {}", dataSourceProvider().database(), isolationLevelName);
                    return;
                }
                prepareConnection(aliceConnection);

                List<Number> salaries = selectColumnList(aliceConnection, allEmployeeSalarySql(), Number.class);
                assertEquals(90_000, salaries.stream().mapToInt(Number::intValue).sum());

                try {
                    executeSync(() -> {
                        doInJDBC(bobConnection -> {
                            prepareConnection(bobConnection);
                            try {
                                List<Number> _salaries = selectColumnList(bobConnection, allEmployeeSalarySql(), Number.class);
                                assertEquals(90_000, _salaries.stream().mapToInt(Number::intValue).sum());

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
                                }
                            } catch (Exception e) {
                                if( ExceptionUtil.isLockTimeout( e )) {
                                    preventedByLocking.set( true );
                                } else if( ExceptionUtil.isMVCCAnomalyDetection( e )) {
                                    preventedByMVCC.set( true );
                                } else {
                                    throw new IllegalStateException( e );
                                }
                            }
                        });
                    });
                } catch (Exception e) {
                    if( ExceptionUtil.isLockTimeout( e )) {
                        preventedByLocking.set( true );
                    } else if( ExceptionUtil.isMVCCAnomalyDetection( e )) {
                        preventedByMVCC.set( true );
                    } else {
                        throw new IllegalStateException( e );
                    }
                }
                update(aliceConnection, updateEmployeeSalarySql());
            });
        } catch (Exception e) {
            if( ExceptionUtil.isLockTimeout( e )) {
                preventedByLocking.set( true );
            } else if( ExceptionUtil.isMVCCAnomalyDetection( e )) {
                preventedByMVCC.set( true );
            } else {
                throw new IllegalStateException( e );
            }
        }
        doInJDBC(aliceConnection -> {
            long salaryCount = selectColumn(aliceConnection, sumEmployeeSalarySql(), Number.class).longValue();
            if(99_000 != salaryCount) {
                LOGGER.info("Isolation level {} allows Phantom Read since the salary count is {} instead of 99000", isolationLevelName, salaryCount);
            }
            else {
                LOGGER.info("Isolation level {} prevents Phantom Read due to {}", isolationLevelName, preventedByLocking.get() ? "locking" : preventedByMVCC.get() ? "MVCC" : "unknown");
            }
        });
    }

    @Test
    public void testPhantomWriteSelectColumnInOneTx() {
        final AtomicBoolean preventedByLocking = new AtomicBoolean();
        final AtomicBoolean preventedByMVCC = new AtomicBoolean();

        try {
            doInJDBC(aliceConnection -> {
                if (!aliceConnection.getMetaData().supportsTransactionIsolationLevel(isolationLevel)) {
                    LOGGER.info("Database {} doesn't support {}", dataSourceProvider().database(), isolationLevelName);
                    return;
                }
                prepareConnection(aliceConnection);

                List<Number> salaries = selectColumnList(aliceConnection, allEmployeeSalarySql(), Number.class);
                assertEquals(90_000, salaries.stream().mapToInt(Number::intValue).sum());

                try {
                    executeSync(() -> {
                        doInJDBC(bobConnection -> {
                            prepareConnection(bobConnection);
                            try {
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
                                }
                            } catch (Exception e) {
                                if( ExceptionUtil.isLockTimeout( e )) {
                                    preventedByLocking.set( true );
                                } else if( ExceptionUtil.isMVCCAnomalyDetection( e )) {
                                    preventedByMVCC.set( true );
                                } else {
                                    throw new IllegalStateException( e );
                                }
                            }
                        });
                    });
                } catch (Exception e) {
                    if( ExceptionUtil.isLockTimeout( e )) {
                        preventedByLocking.set( true );
                    } else if( ExceptionUtil.isMVCCAnomalyDetection( e )) {
                        preventedByMVCC.set( true );
                    } else {
                        throw new IllegalStateException( e );
                    }
                }
                update(aliceConnection, updateEmployeeSalarySql());
            });
        } catch (Exception e) {
            if( ExceptionUtil.isLockTimeout( e )) {
                preventedByLocking.set( true );
            } else if( ExceptionUtil.isMVCCAnomalyDetection( e )) {
                preventedByMVCC.set( true );
            } else {
                throw new IllegalStateException( e );
            }
        }
        doInJDBC(aliceConnection -> {
            long salaryCount = selectColumn(aliceConnection, sumEmployeeSalarySql(), Number.class).longValue();
            if(99_000 != salaryCount) {
                LOGGER.info("Isolation level {} allows Phantom Read since the salary count is {} instead of 99000", isolationLevelName, salaryCount);
            }
            else {
                LOGGER.info("Isolation level {} prevents Phantom Read due to {}", isolationLevelName, preventedByLocking.get() ? "locking" : preventedByMVCC.get() ? "MVCC" : "unknown");
            }
        });
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
    @Table(name = "employee", indexes = @Index(name = "IDX_Employee", columnList = "department_id"))
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
