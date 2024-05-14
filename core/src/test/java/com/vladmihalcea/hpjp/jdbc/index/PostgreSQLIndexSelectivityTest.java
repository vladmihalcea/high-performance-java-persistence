package com.vladmihalcea.hpjp.jdbc.index;

import com.vladmihalcea.hpjp.util.AbstractPostgreSQLIntegrationTest;
import com.vladmihalcea.hpjp.util.providers.Database;
import com.vladmihalcea.hpjp.util.providers.queries.PostgreSQLQueries;
import jakarta.persistence.*;
import org.hibernate.annotations.JdbcType;
import org.hibernate.dialect.PostgreSQLEnumJdbcType;
import org.junit.Test;
import org.postgresql.util.PGobject;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

import static org.junit.Assert.*;

/**
 * PostgresIndexSelectivityTest - Test PostgreSQL index selectivity
 *
 * @author Vlad Mihalcea
 */
public class PostgreSQLIndexSelectivityTest extends AbstractPostgreSQLIntegrationTest {

    public static final String INSERT_TASK = "INSERT INTO task (id, name, status) VALUES (?, ?, ?)";

    @Override
    protected Class<?>[] entities() {
        return new Class[] {
            Task.class
        };
    }

    @Override
    protected void beforeInit() {
        executeStatement("DROP TYPE IF EXISTS task_status");
        executeStatement("CREATE TYPE task_status AS ENUM ('TO_DO', 'DONE', 'FAILED')");
    }

    @Override
    protected Database database() {
        return Database.POSTGRESQL;
    }

    @Override
    protected void additionalProperties(Properties properties) {
        properties.put("hibernate.jdbc.batch_size", "50");
    }

    @Test
    public void testSelectivity() {
        AtomicInteger statementCount = new AtomicInteger();
        long startNanos = System.nanoTime();
        doInJDBC(connection -> {
            try (PreparedStatement statement = connection.prepareStatement(INSERT_TASK)) {
                int taskCount = getTaskCount();

                for (int i = 1; i <= taskCount; i++) {
                    Task.Status status = Task.Status.DONE;
                    if (i > 99000) {
                        status = Task.Status.TO_DO;
                    } else if (i > 95000) {
                        status = Task.Status.FAILED;
                    }
                    statement.setLong(1, i);
                    statement.setString(2, String.format("Task %d", i));
                    statement.setObject(3, PostgreSQLQueries.toEnum(status, "task_status"), Types.OTHER);
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
        executeStatement("CREATE INDEX IF NOT EXISTS idx_task_status ON task (status)");
        executeStatement("VACUUM ANALYZE");

        doInJDBC(connection -> {
            printExecutionPlanForSelectByStatus(connection, Task.Status.DONE);
            printExecutionPlanForSelectByStatus(connection, Task.Status.TO_DO);
        });

        LOGGER.info("Using a Partial Index");

        executeStatement("DROP INDEX IF EXISTS idx_task_status");
        executeStatement("CREATE INDEX idx_task_status ON task (status) WHERE status <> 'DONE'");
        executeStatement("VACUUM ANALYZE");

        doInJDBC(connection -> {
            printExecutionPlanForSelectByStatus(connection, Task.Status.DONE);
            printExecutionPlanForSelectByStatus(connection, Task.Status.TO_DO);
        });
    }

    private void executeStatement(PreparedStatement statement, AtomicInteger statementCount) throws SQLException {
        statement.addBatch();
        int count = statementCount.incrementAndGet();
        if(count % getBatchSize() == 0) {
            statement.executeBatch();
        }
    }

    protected int getTaskCount() {
        return 100 * 1000;
    }

    protected int getBatchSize() {
        return 100;
    }

    private void printExecutionPlanForSelectByStatus(Connection connection, Task.Status status) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement("""
                EXPLAIN ANALYZE
                SELECT *
                FROM task
                WHERE status = ?
                """
        )) {

            assertFalse(PostgreSQLQueries.isUseServerPrepare(statement));
            PostgreSQLQueries.setPrepareThreshold(statement, 1);
            statement.setObject(1, PostgreSQLQueries.toEnum(status, "task_status"), Types.OTHER);
            ResultSet resultSet = statement.executeQuery();

            List<String> planLines = new ArrayList<>();
            while (resultSet.next()) {
                planLines.add(resultSet.getString(1));
            }
            LOGGER.info("Execution plan: {}{}",
                System.lineSeparator(),
                planLines.stream().collect(Collectors.joining(System.lineSeparator()))
            );

            assertTrue(PostgreSQLQueries.isUseServerPrepare(statement));
        }
    }

    @Entity(name = "Task")
    @Table(name = "task")
    public static class Task {

        @Id
        @GeneratedValue(strategy = GenerationType.SEQUENCE)
        private Long id;

        @Column(length = 50)
        private String name;

        @Column(columnDefinition = "task_status")
        @JdbcType(PostgreSQLEnumJdbcType.class)
        private Task.Status status;

        public Long getId() {
            return id;
        }

        public Task setId(Long id) {
            this.id = id;
            return this;
        }

        public String getName() {
            return name;
        }

        public Task setName(String name) {
            this.name = name;
            return this;
        }

        public Task.Status getStatus() {
            return status;
        }

        public Task setStatus(Task.Status status) {
            this.status = status;
            return this;
        }

        public enum Status {
            DONE,
            TO_DO,
            FAILED;

            public static Task.Status random() {
                ThreadLocalRandom random = ThreadLocalRandom.current();
                Task.Status[] values = Task.Status.values();
                return values[random.nextInt(values.length)];
            }
        }
    }
}
