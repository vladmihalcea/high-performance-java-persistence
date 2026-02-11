package com.vladmihalcea.hpjp.jdbc.index;

import com.vladmihalcea.hpjp.util.AbstractTest;
import com.vladmihalcea.hpjp.util.providers.Database;
import com.vladmihalcea.hpjp.util.providers.queries.PostgreSQLQueries;
import jakarta.persistence.*;
import org.hibernate.Session;
import org.junit.jupiter.api.Test;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.fail;

/**
 * SQLServerIndexSelectivityTest - Test SQL Server index selectivity
 *
 * @author Vlad Mihalcea
 */
public class SQLServerIndexSelectivityTest extends AbstractTest {

    public static final String INSERT_TASK = "INSERT INTO Tasks (Id, Name, Status) VALUES (?, ?, ?)";

    @Override
    protected Class<?>[] entities() {
        return new Class[] {
            Task.class
        };
    }

    private boolean initialize;

    @Override
    protected void beforeInit() {
        Integer taskCount = selectColumn("select count(*) from Tasks", Integer.class);
        initialize = (taskCount == null || taskCount != getTaskCount());
        if(initialize) {

            executeStatement("drop table Tasks");
            executeStatement("drop sequence Tasks_SEQ");
            executeStatement("create sequence Tasks_SEQ start with 1 increment by 50");
            executeStatement("create table Tasks (Id bigint not null, Name varchar(50), Status varchar(255) check ((Status in ('DONE','TO_DO','FAILED'))), primary key (Id))");
        }
    }

    @Override
    protected Database database() {
        return Database.SQLSERVER;
    }

    @Override
    protected void additionalProperties(Properties properties) {
        properties.put("hibernate.hbm2ddl.auto", "none");
        properties.put("hibernate.jdbc.batch_size", "50");
    }

    @Override
    protected void afterInit() {
        if(initialize) {
            AtomicInteger statementCount = new AtomicInteger();
            long startNanos = System.nanoTime();
            doInJDBC(connection -> {
                try (PreparedStatement statement = connection.prepareStatement(INSERT_TASK)) {
                    int taskCount = getTaskCount();

                    for (int i = 1; i <= taskCount; i++) {
                        Task.Status status = Task.Status.DONE;
                        if (i > 99_000) {
                            status = Task.Status.TO_DO;
                        } else if (i > 97_000) {
                            status = Task.Status.FAILED;
                        } else if (i > 94_000) {
                            status = null;
                        }
                        statement.setLong(1, i);
                        statement.setString(2, String.format("Task %d", i));
                        statement.setString(3, status != null ? status.name() : null);
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
        }
    }

    @Test
    public void testExecutionPlanIndexSelectivity() {
        executeStatement("DROP INDEX IF EXISTS TASKS_STATUS ON Tasks");
        executeStatement("""
            CREATE NONCLUSTERED INDEX TASKS_STATUS
            ON Tasks (Status)
            WHERE Status IS NOT NULL
            """);
        executeStatement("UPDATE STATISTICS Tasks WITH FULLSCAN");

        doInJDBC(connection -> {
            for (int i = 0; i < 100; i++) {
                selectByStatus(connection, Task.Status.DONE);
            }
            printExecutionPlanForSelectByStatus(connection, Task.Status.TO_DO);
        });
    }

    @Test
    public void testWithoutColumnStoreIndex() {
        executeStatement("DROP INDEX IF EXISTS TASKS_STATUS ON Tasks");
        executeStatement("""
            CREATE NONCLUSTERED INDEX TASKS_STATUS
            ON Tasks (Status)
            WHERE Status IS NOT NULL
            """);
        executeStatement("UPDATE STATISTICS Tasks WITH FULLSCAN");

        testDistinctQueryOnTaskStatus();
    }

    @Test
    public void testWithColumnStoreIndex() {
        executeStatement("DROP INDEX IF EXISTS TASKS_STATUS ON Tasks");
        executeStatement("""
            CREATE NONCLUSTERED COLUMNSTORE INDEX TASKS_STATUS
            ON Tasks (Status) WHERE Status IS NOT NULL
            """);
        executeStatement("UPDATE STATISTICS Tasks WITH FULLSCAN");

        testDistinctQueryOnTaskStatus();
    }

    private void testDistinctQueryOnTaskStatus() {
        //Check Execution Plan
        doInJDBC(connection -> {
            List<Map<String, String>> planLines = new ArrayList<>();

            try (Statement statement = connection.createStatement();) {
                statement.executeUpdate(
                    "SET STATISTICS IO, TIME, PROFILE ON"
                );

                boolean moreResultSets = statement.execute("""
                    SELECT DISTINCT Status
                    FROM Tasks
                    WHERE Status IS NOT NULL
                    """);

                while (moreResultSets) {
                    planLines.addAll(parseResultSet(statement.getResultSet()));

                    moreResultSets = statement.getMoreResults();
                }

                statement.executeUpdate(
                    "SET STATISTICS IO, TIME, PROFILE OFF"
                );

                LOGGER.info("Execution plan: {}{}",
                    System.lineSeparator(),
                    planLines.stream().map(Map::toString).collect(Collectors.joining(System.lineSeparator()))
                );
            }
        });

        doInJDBC(connection -> {
            List<String> statuses = new ArrayList<>();

            long startNanos = System.nanoTime();
            try (Statement statement = connection.createStatement();) {
                ResultSet resultSet = statement.executeQuery("""
                    SELECT DISTINCT Status
                    FROM Tasks
                    WHERE Status IS NOT NULL
                    """);

                while (resultSet.next()) {
                    statuses.add(resultSet.getString(1));
                }

                LOGGER.info("Fetching the task statuses: [{}] took: [{}] ms", statuses, TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - startNanos));
            }
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
        return 1000 * 1000;
    }

    protected int getBatchSize() {
        return 1000;
    }

    private void printExecutionPlanForSelectByStatus(Connection connection, Task.Status status) throws SQLException {
        List<Map<String, String>> planLines = new ArrayList<>();

        try (PreparedStatement preparedStatement = connection.prepareStatement("""
                SELECT *
                FROM Tasks
                WHERE Status = ?
                """);
             Statement statement = connection.createStatement();
        ) {
            statement.executeUpdate(
                "SET STATISTICS IO, TIME, PROFILE ON"
            );

            preparedStatement.setString(1, status.name());
            boolean moreResultSets = preparedStatement.execute();

            while (moreResultSets) {
                planLines.addAll(parseResultSet(preparedStatement.getResultSet()));

                moreResultSets = preparedStatement.getMoreResults();
            }

            statement.executeUpdate(
                "SET STATISTICS IO, TIME, PROFILE OFF"
            );
        }

        LOGGER.info("Execution plan: {}{}",
            System.lineSeparator(),
            planLines.stream().map(Map::toString).collect(Collectors.joining(System.lineSeparator()))
        );
    }

    private List<String> selectByStatus(Connection connection, Task.Status status) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement("""
            SELECT *
            FROM Tasks
            WHERE Status = ?
            """)) {
            statement.setString(1, status.name());
            ResultSet resultSet = statement.executeQuery();

            List<String> result = new ArrayList<>();
            while (resultSet.next()) {
                result.add(resultSet.getString(1));
            }
            return result;
        }
    }

    @Entity(name = "Task")
    @Table(name = "Tasks")
    public static class Task {

        @Id
        @GeneratedValue(strategy = GenerationType.SEQUENCE)
        @Column(name = "Id")
        private Long id;

        @Column(name = "Name", length = 50)
        private String name;

        @Enumerated(EnumType.STRING)
        @Column(name = "Status")
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
