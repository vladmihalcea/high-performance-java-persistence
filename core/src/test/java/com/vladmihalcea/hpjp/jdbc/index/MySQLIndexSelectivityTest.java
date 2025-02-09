package com.vladmihalcea.hpjp.jdbc.index;

import com.vladmihalcea.hpjp.util.AbstractTest;
import com.vladmihalcea.hpjp.util.providers.DataSourceProvider;
import com.vladmihalcea.hpjp.util.providers.Database;
import com.vladmihalcea.hpjp.util.providers.MySQLDataSourceProvider;
import jakarta.persistence.*;
import org.junit.Test;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

import static org.junit.Assert.fail;

/**
 * PostgresIndexSelectivityTest - Test PostgreSQL index selectivity
 *
 * @author Vlad Mihalcea
 */
public class MySQLIndexSelectivityTest extends AbstractTest {

    public static final String INSERT_TASK = "INSERT INTO task (id, name, status) VALUES (?, ?, ?)";

    @Override
    protected Class<?>[] entities() {
        return new Class[] {
            Task.class
        };
    }

    @Override
    protected DataSourceProvider dataSourceProvider() {
        return new MySQLDataSourceProvider()
            .setRewriteBatchedStatements(true);
    }

    @Test
    public void testInsert() {
        AtomicInteger statementCount = new AtomicInteger();
        long startNanos = System.nanoTime();
        doInJDBC(connection -> {
            try (PreparedStatement statement = connection.prepareStatement(INSERT_TASK)) {
                int taskCount = getTaskCount();

                for (int i = 0; i < taskCount; i++) {
                    String status = Task.Status.DONE.name();
                    if (i >= (0.99 * taskCount)) {
                        status = Task.Status.TO_DO.name();
                    } else if (i >= (0.95 * taskCount)) {
                        status = Task.Status.FAILED.name();
                    }
                    statement.setLong(1, i);
                    statement.setString(2, String.format("Task %d", i));
                    statement.setString(3, status);
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

        boolean createIndex = false;

        if (createIndex) {
            executeStatement("CREATE INDEX idx_task_status ON task (status)");
            executeStatement("OPTIMIZE TABLE task");
        }

        boolean printExecutionPlans = false;

        doInJDBC(connection -> {
            executeQueryWithSkewedFilterPredicate(connection, Task.Status.DONE, printExecutionPlans);
            executeQueryWithSkewedFilterPredicate(connection, Task.Status.TO_DO, printExecutionPlans);
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
        return 10_000 * 1_000;
    }

    protected int getBatchSize() {
        return 1000;
    }

    private void executeQueryWithSkewedFilterPredicate(Connection connection, Task.Status status, boolean printExecutionPlans) throws SQLException {
        String query = String.format("""
            %sSELECT id, name
            FROM task
            WHERE status = ?
            """,
            printExecutionPlans ? "EXPLAIN FORMAT=JSON " : ""
        );

        try (PreparedStatement statement = connection.prepareStatement(query)) {
            statement.setString(1, status.name());
            ResultSet resultSet = statement.executeQuery();

            if (printExecutionPlans) {
                List<String> planLines = new ArrayList<>();
                while (resultSet.next()) {
                    planLines.add(resultSet.getString(1));
                }
                LOGGER.info("Execution plan: {}{}",
                    System.lineSeparator(),
                    planLines.stream().collect(Collectors.joining(System.lineSeparator()))
                );
            } else {
                int rowCount = 0;
                while (resultSet.next()) {
                    rowCount++;
                }
                LOGGER.info("Fetched [{}] records for [{}] status",
                    rowCount,
                    status
                );
            }
        }
    }

    @Entity(name = "Task")
    @Table(name = "task")
    public static class Task {

        @Id
        private Long id;

        private String name;

        @Enumerated(EnumType.STRING)
        private Status status;

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

        public Status getStatus() {
            return status;
        }

        public Task setStatus(Status status) {
            this.status = status;
            return this;
        }

        public enum Status {
            DONE,
            TO_DO,
            FAILED;

            public static Status random() {
                ThreadLocalRandom random = ThreadLocalRandom.current();
                Status[] values = Status.values();
                return values[random.nextInt(values.length)];
            }
        }
    }
}
