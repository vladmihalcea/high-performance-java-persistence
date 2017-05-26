package com.vladmihalcea.book.hpjp.jdbc.transaction.phenomena.linearizabilty;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import org.junit.Test;
import org.junit.runners.Parameterized;

import com.vladmihalcea.book.hpjp.util.providers.DataSourceProvider;
import com.vladmihalcea.book.hpjp.util.providers.OracleDataSourceProvider;

import static org.junit.Assert.assertEquals;

/**
 * OraclePhenomenaTest - Test to validate Oracle phenomena
 *
 * @author Vlad Mihalcea
 */
public class OracleLinearizabilityPhenomenaTest extends AbstractLinearizabilityPhenomenaTest {

    public OracleLinearizabilityPhenomenaTest(String isolationLevelName, int isolationLevel) {
        super(isolationLevelName, isolationLevel);
    }

    @Parameterized.Parameters
    public static Collection<Object[]> isolationLevels() {
        List<Object[]> levels = new ArrayList<>();
        levels.add(new Object[]{"Read Committed", Connection.TRANSACTION_READ_COMMITTED});
        levels.add(new Object[]{"Serializable", Connection.TRANSACTION_SERIALIZABLE});
        return levels;
    }

    @Override
    protected DataSourceProvider dataSourceProvider() {
        return new OracleDataSourceProvider();
    }

    @Override
    public void init() {
        super.init();
        doInJDBC(aliceConnection -> {
            executeStatement(aliceConnection, "alter table employee initrans 100");
        });
    }

    @Test
    public void testPhantomWriteAggregateNTimes() {
        if (isolationLevel != Connection.TRANSACTION_SERIALIZABLE) {
            return;
        }

        int sleepMillis = 100;

        AtomicInteger ok = new AtomicInteger();
        AtomicInteger fail = new AtomicInteger();
        for (int i = 0; i < 10; i++) {
            AtomicReference<Boolean> preventedByLocking = new AtomicReference<>();

            doInJDBC(aliceConnection -> {
                executeStatement(aliceConnection, "delete from employee where id = 4");
                executeStatement(aliceConnection, "update employee set salary = 30000");
            });

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
                                    LOGGER.info("Exception thrown", e);
                                    preventedByLocking.set(true);
                                }
                            });
                        });
                    } catch (Exception e) {
                        LOGGER.info("Exception thrown", e);
                        preventedByLocking.set(true);
                    }
                    sleep(sleepMillis);
                    update(aliceConnection, "UPDATE employee SET salary = salary * 1.1 WHERE department_id = 1 and id < 4");
                });
            } catch (Exception e) {
                LOGGER.info("Exception thrown", e);
                preventedByLocking.set(true);
            }
            doInJDBC(aliceConnection -> {
                long salaryCount = selectColumn(aliceConnection, sumEmployeeSalarySql(), Number.class).longValue();
                if(99_000 != salaryCount) {
                    LOGGER.info("Isolation level {} allows Phantom Write since the salary count is {} instead of 99000", isolationLevelName, salaryCount);
                    fail.incrementAndGet();
                }
                else {
                    LOGGER.info("Isolation level {} prevents Phantom Write {}", isolationLevelName, preventedByLocking.get() ? "due to locking" : "");
                    ok.incrementAndGet();
                }
            });
            LOGGER.info("Success: {}, fail: {}", ok.get(), fail.get());
        }
    }
}
