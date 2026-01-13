package com.vladmihalcea.hpjp.jdbc.transaction.phenomena.writeskew;

import com.vladmihalcea.hpjp.util.providers.DataSourceProvider;
import com.vladmihalcea.hpjp.util.providers.OracleDataSourceProvider;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedClass;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.time.Duration;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * @author Vlad Mihalcea
 */
@ParameterizedClass
@MethodSource("parameters")
public class OracleRangeBasedWriteSkewPhenomenaTest extends AbstractRangeBasedWriteSkewPhenomenaTest {

    public static Stream<Arguments> parameters() {
        return Stream.of(
            Arguments.of("Read Committed", Connection.TRANSACTION_READ_COMMITTED),
            Arguments.of("Serializable", Connection.TRANSACTION_SERIALIZABLE)
        );
    }

    @Override
    protected DataSourceProvider dataSourceProvider() {
        return new OracleDataSourceProvider();
    }

    @Override
    public void afterInit() {
        super.afterInit();
        doInJDBC(aliceConnection -> {
            executeStatement(aliceConnection, "alter table employee initrans 100");
        });
    }

    @Test
    public void testWriteSkewAggregateNTimes() {
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
                long salaryCount = selectColumn(aliceConnection, sumEmployeeSalarySql(), Number.class, Duration.ofSeconds(1)).longValue();
                if(99_000 != salaryCount) {
                    LOGGER.info("Isolation level {} allows Write Skew since the salary count is {} instead of 99000", isolationLevelName, salaryCount);
                    fail.incrementAndGet();
                }
                else {
                    LOGGER.info("Isolation level {} prevents Write Skew {}", isolationLevelName, preventedByLocking.get() ? "due to locking" : "");
                    ok.incrementAndGet();
                }
            });
            LOGGER.info("Success: {}, fail: {}", ok.get(), fail.get());
        }
    }
}
