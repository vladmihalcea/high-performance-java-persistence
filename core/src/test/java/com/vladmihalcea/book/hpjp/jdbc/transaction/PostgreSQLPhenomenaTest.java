package com.vladmihalcea.book.hpjp.jdbc.transaction;

import org.junit.Test;

import java.sql.PreparedStatement;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.Assert.assertEquals;

/**
 * PostgreSQLPhenomenaTest - Test to validate PostgreSQL phenomena
 *
 * @author Vlad Mihalcea
 */
public class PostgreSQLPhenomenaTest extends AbstractPhenomenaTest {

    public PostgreSQLPhenomenaTest(String isolationLevelName, int isolationLevel) {
        super(isolationLevelName, isolationLevel);
    }

    @Override
    protected String lockEmployeeTableSql() {
        return "LOCK TABLE employee IN SHARE ROW EXCLUSIVE MODE NOWAIT";
    }

    @Override
    protected DataSourceProvider dataSourceProvider() {
        return new PostgreSQLDataSourceProvider();
    }

    @Test
    public void testPhantomWriteSelectColumnInOneTx() {
        AtomicReference<Boolean> preventedByLocking = new AtomicReference<>();
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
                                LOGGER.info("Exception thrown", e);
                                preventedByLocking.set(true);
                            }
                        });
                    });
                } catch (Exception e) {
                    LOGGER.info("Exception thrown", e);
                    preventedByLocking.set(true);
                }
                update(aliceConnection, updateEmployeeSalarySql());
            });
        } catch (Exception e) {
            LOGGER.info("Exception thrown", e);
            preventedByLocking.set(true);
        }
        doInJDBC(aliceConnection -> {
            long salaryCount = selectColumn(aliceConnection, sumEmployeeSalarySql(), Number.class).longValue();
            if(99_000 != salaryCount) {
                LOGGER.info("Isolation level {} allows Phantom Write since the salary count is {} instead 99000", isolationLevelName, salaryCount);
            }
            else {
                LOGGER.info("Isolation level {} prevents Phantom Write {}", isolationLevelName, preventedByLocking.get() ? "due to locking" : "");
            }
        });
    }
}
