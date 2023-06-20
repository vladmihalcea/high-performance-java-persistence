package com.vladmihalcea.hpjp.jdbc.transaction.phenomena.writeskew;

import com.vladmihalcea.hpjp.util.exception.ExceptionUtil;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import java.sql.PreparedStatement;
import java.time.Duration;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.junit.Assert.assertEquals;

/**
 * @author Vlad Mihalcea
 */
@RunWith(Parameterized.class)
public abstract class AbstractRangeBasedWriteSkewPhenomenaTest extends AbstractDepartmentEmployeePhenomenaTest {

    protected AbstractRangeBasedWriteSkewPhenomenaTest(String isolationLevelName, int isolationLevel) {
        super(isolationLevelName, isolationLevel);
    }

    @Test
    public void testWriteSkewAggregateWriteSkewAggregate() {
        final AtomicBoolean preventedByLocking = new AtomicBoolean();
        final AtomicBoolean preventedByMVCC = new AtomicBoolean();

        try {
            doInJDBC(aliceConnection -> {
                if (!aliceConnection.getMetaData().supportsTransactionIsolationLevel(isolationLevel)) {
                    LOGGER.info("Database {} doesn't support {}", dataSourceProvider().database(), isolationLevelName);
                    return;
                }
                prepareConnection(aliceConnection);
                long salaryCount = selectColumn(aliceConnection, sumEmployeeSalarySql(), Number.class, Duration.ofSeconds(1)).longValue();
                assertEquals(90_000, salaryCount);

                try {
                    executeSync(() -> {
                        doInJDBC(bobConnection -> {
                            prepareConnection(bobConnection);
                            try {
                                long _salaryCount = selectColumn(bobConnection, sumEmployeeSalarySql(), Number.class, Duration.ofSeconds(1)).longValue();
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
            long salaryCount = selectColumn(aliceConnection, sumEmployeeSalarySql(), Number.class, Duration.ofSeconds(1)).longValue();
            if(99_000 != salaryCount) {
                LOGGER.info("Isolation level {} allows Write Skew since the salary count is {} instead of 99000", isolationLevelName, salaryCount);
            }
            else {
                LOGGER.info("Isolation level {} prevents Write Skew due to {}", isolationLevelName, preventedByLocking.get() ? "locking" : preventedByMVCC.get() ? "MVCC" : "unknown");
            }
        });
    }

    @Test
    public void testWriteSkewAggregateWithInsert() {
        final AtomicBoolean preventedByLocking = new AtomicBoolean();
        final AtomicBoolean preventedByMVCC = new AtomicBoolean();

        try {
            doInJDBC(aliceConnection -> {
                if (!aliceConnection.getMetaData().supportsTransactionIsolationLevel(isolationLevel)) {
                    LOGGER.info("Database {} doesn't support {}", dataSourceProvider().database(), isolationLevelName);
                    return;
                }
                prepareConnection(aliceConnection);
                long salaryCount = selectColumn(aliceConnection, sumEmployeeSalarySql(), Number.class, Duration.ofSeconds(1)).longValue();
                assertEquals(90_000, salaryCount);

                try {
                    executeSync(() -> {
                        doInJDBC(bobConnection -> {
                            prepareConnection(bobConnection);
                            try {
                                long _salaryCount = selectColumn(bobConnection, sumEmployeeSalarySql(), Number.class, Duration.ofSeconds(1)).longValue();
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
            long salaryCount = selectColumn(aliceConnection, sumEmployeeSalarySql(), Number.class, Duration.ofSeconds(1)).longValue();
            if(99_000 != salaryCount) {
                LOGGER.info("Isolation level {} allows Write Skew since the salary count is {} instead of 99000", isolationLevelName, salaryCount);
            }
            else {
                LOGGER.info("Isolation level {} prevents Write Skew due to {}", isolationLevelName, preventedByLocking.get() ? "locking" : preventedByMVCC.get() ? "MVCC" : "unknown");
            }
        });
    }

    @Test
    public void testWriteSkewSelectColumn() {
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
            long salaryCount = selectColumn(aliceConnection, sumEmployeeSalarySql(), Number.class, Duration.ofSeconds(1)).longValue();
            if(99_000 != salaryCount) {
                LOGGER.info("Isolation level {} allows Write Skew since the salary count is {} instead of 99000", isolationLevelName, salaryCount);
            }
            else {
                LOGGER.info("Isolation level {} prevents Write Skew due to {}", isolationLevelName, preventedByLocking.get() ? "locking" : preventedByMVCC.get() ? "MVCC" : "unknown");
            }
        });
    }

    @Test
    public void testWriteSkewSelectColumnInOneTx() {
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
            long salaryCount = selectColumn(aliceConnection, sumEmployeeSalarySql(), Number.class, Duration.ofSeconds(1)).longValue();
            if(99_000 != salaryCount) {
                LOGGER.info("Isolation level {} allows Write Skew since the salary count is {} instead of 99000", isolationLevelName, salaryCount);
            }
            else {
                LOGGER.info("Isolation level {} prevents Write Skew due to {}", isolationLevelName, preventedByLocking.get() ? "locking" : preventedByMVCC.get() ? "MVCC" : "unknown");
            }
        });
    }
}
