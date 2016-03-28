package com.vladmihalcea.book.hpjp.jdbc.index;

import com.vladmihalcea.book.hpjp.jdbc.index.providers.IndexEntityProvider;
import com.vladmihalcea.book.hpjp.util.AbstractPostgreSQLIntegrationTest;
import org.junit.Test;
import org.postgresql.PGStatement;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Proxy;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.Assert.*;

/**
 * PostgresIndexSelectivityTest - Test PostgreSQL index selectivity
 *
 * @author Vlad Mihalcea
 */
public class PostgresIndexSelectivityTest extends AbstractPostgreSQLIntegrationTest {

    public static final String INSERT_TASK = "insert into Task (id, status) values (?, ?)";

    private final IndexEntityProvider entityProvider = new IndexEntityProvider();

    @Override
    protected Class<?>[] entities() {
        return entityProvider.entities();
    }

    @Test
    public void testInsert() {
        AtomicInteger statementCount = new AtomicInteger();
        long startNanos = System.nanoTime();
        doInJDBC(connection -> {
            try (PreparedStatement statement = connection.prepareStatement(INSERT_TASK)) {
                int taskCount = getPostCount();

                for (int i = 0; i < taskCount; i++) {
                    String task = "DONE";
                    if (i > 99000) {
                        task = "TO_DO";
                    } else if (i > 95000) {
                        task = "FAILED";
                    }
                    statement.setLong(1, i);
                    statement.setString(2, task);
                    executeStatement(statement, statementCount);
                }
                statement.executeBatch();
            } catch (SQLException e) {
                fail(e.getMessage());
            }
        });
        LOGGER.info("{}.testInsert took {} millis",
                getClass().getSimpleName(),
                TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - startNanos));
        doInJDBC(connection -> {
            try (PreparedStatement statement = connection.prepareStatement(
                    "select * from task where id = ?"
            )) {

                assertFalse(isUseServerPrepare(statement));
                setPrepareThreshold(statement, 1);
                statement.setInt(1, 100);
                statement.execute();
                assertTrue(isUseServerPrepare(statement));
            }
        });
    }

    public boolean isUseServerPrepare(Statement statement) {
        if(statement instanceof PGStatement) {
            PGStatement pgStatement = (PGStatement) statement;
            return pgStatement.isUseServerPrepare();
        } else {
            InvocationHandler handler = Proxy.getInvocationHandler(statement);
            try {
                return (boolean) handler.invoke(statement, PGStatement.class.getMethod("isUseServerPrepare"), null);
            } catch (Throwable e) {
                throw new IllegalArgumentException(e);
            }
        }
    }

    public void setPrepareThreshold(Statement statement, int threshold) throws SQLException {
        if(statement instanceof PGStatement) {
            PGStatement pgStatement = (PGStatement) statement;
            pgStatement.setPrepareThreshold(threshold);
        } else {
            InvocationHandler handler = Proxy.getInvocationHandler(statement);
            try {
                handler.invoke(statement, PGStatement.class.getMethod("setPrepareThreshold", int.class), new Object[]{threshold});
            } catch (Throwable throwable) {
                throw new IllegalArgumentException(throwable);
            }
        }
    }

    private void executeStatement(PreparedStatement statement, AtomicInteger statementCount) throws SQLException {
        statement.addBatch();
        int count = statementCount.incrementAndGet();
        if(count % getBatchSize() == 0) {
            statement.executeBatch();
        }
    }

    protected int getPostCount() {
        return 1 * 1000;
    }

    protected int getBatchSize() {
        return 100;
    }
}
