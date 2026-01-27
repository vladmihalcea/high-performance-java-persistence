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

    private boolean initialize;

    @Override
    protected void beforeInit() {
        Long taskCount = selectColumn("select count(*) from task", Long.class);
        initialize = (taskCount == null || taskCount.intValue() != getTaskCount());
        if(initialize) {
            executeStatement("DROP TYPE IF EXISTS task_status");
            executeStatement("CREATE TYPE task_status AS ENUM ('TO_DO', 'DONE', 'FAILED')");

            executeStatement("drop table if exists task cascade");
            executeStatement("drop sequence if exists task_SEQ");
            executeStatement("create sequence task_SEQ start with 1 increment by 50");
            executeStatement("create table task (id bigint not null, name varchar(50), status task_status, primary key (id))");
        }
    }

    @Override
    protected Database database() {
        return Database.POSTGRESQL;
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
        }
    }

    @Test
    public void testExecutionPlanIndexSelectivity() {
        executeStatement("CREATE INDEX IF NOT EXISTS idx_task_status ON task (status)");
        executeStatement("VACUUM ANALYZE");

        doInJDBC(connection -> {
            printExecutionPlanForSelectByStatus(connection, Task.Status.DONE);
            printExecutionPlanForSelectByStatus(connection, Task.Status.TO_DO);
        });

        LOGGER.info("Using a Partial Index");

        executeStatement("DROP INDEX IF EXISTS idx_task_status");
        executeStatement("CREATE INDEX idx_task_status ON task (status) WHERE status <> 'DONE'");

        doInJDBC(connection -> {
            printExecutionPlanForSelectByStatus(connection, Task.Status.DONE);
            printExecutionPlanForSelectByStatus(connection, Task.Status.TO_DO);
        });
    }

    /**
     * 2026-01-27 14:44:56.948 EET [24680] LOG:  duration: 53.621 ms  plan:
     * 	Query Text: SELECT *
     * 	FROM task
     * 	WHERE status = $1
     *
     * 	Seq Scan on task  (cost=0.00..1887.00 rows=33333 width=22) (actual time=0.037..12.369 rows=95000 loops=1)
     * 	  Filter: (status = $1)
     * 	  Rows Removed by Filter: 5000
     * 2026-01-27 14:44:56.948 EET [24680] LOG:  execute S_1: SELECT *
     * 	FROM task
     * 	WHERE status = $1
     *
     * 2026-01-27 14:44:56.948 EET [24680] DETAIL:  parameters: $1 = 'TO_DO'
     * 2026-01-27 14:44:56.960 EET [24680] LOG:  duration: 10.332 ms  plan:
     * 	Query Text: SELECT *
     * 	FROM task
     * 	WHERE status = $1
     *
     * 	Seq Scan on task  (cost=0.00..1887.00 rows=33333 width=22) (actual time=9.646..9.796 rows=1000 loops=1)
     * 	  Filter: (status = $1)
     * 	  Rows Removed by Filter: 99000
     * 2026-01-27 14:44:56.960 EET [24680] LOG:  execute S_2: COMMIT
     */
    @Test
    public void testPostgreSQLPrepareThreshold () {
        executeStatement("CREATE INDEX IF NOT EXISTS idx_task_status ON task (status)");
        executeStatement("VACUUM ANALYZE");

        doInJDBC(connection -> {
            executeStatement(connection, """
                SET auto_explain.log_min_duration TO 0;
                """);
            for (int i = 0; i < 100; i++) {
                selectByStatus(connection, Task.Status.DONE);
            }
            selectByStatus(connection, Task.Status.TO_DO);
        });
    }

    /**
     * 2026-01-27 14:46:56.795 EET [2088] LOG:  duration: 71.803 ms  plan:
     * 	Query Text: SELECT *
     * 	FROM task
     * 	WHERE status = $1
     *
     * 	Seq Scan on task  (cost=0.00..1887.00 rows=33333 width=22) (actual time=0.028..16.695 rows=95000 loops=1)
     * 	  Filter: (status = $1)
     * 	  Rows Removed by Filter: 5000
     * 2026-01-27 14:46:56.795 EET [2088] LOG:  execute S_1: SELECT *
     * 	FROM task
     * 	WHERE status = $1
     *
     * 2026-01-27 14:46:56.795 EET [2088] DETAIL:  parameters: $1 = 'DONE'
     * 2026-01-27 14:46:56.876 EET [2088] LOG:  duration: 73.520 ms  plan:
     * 	Query Text: SELECT *
     * 	FROM task
     * 	WHERE status = $1
     *
     * 	Seq Scan on task  (cost=0.00..1887.00 rows=33333 width=22) (actual time=0.082..17.294 rows=95000 loops=1)
     * 	  Filter: (status = $1)
     * 	  Rows Removed by Filter: 5000
     * 2026-01-27 14:46:56.876 EET [2088] LOG:  execute <unnamed>: SELECT *
     * 	FROM task
     * 	WHERE status = 'TO_DO'
     *
     * 2026-01-27 14:46:56.879 EET [2088] LOG:  duration: 0.597 ms  plan:
     * 	Query Text: SELECT *
     * 	FROM task
     * 	WHERE status = 'TO_DO'
     *
     * 	Index Scan using idx_task_status on task  (cost=0.28..300.05 rows=1017 width=22) (actual time=0.029..0.152 rows=1000 loops=1)
     * 	  Index Cond: (status = 'TO_DO'::task_status)
     * 2026-01-27 14:46:56.879 EET [2088] LOG:  execute S_2: COMMIT
     */
    @Test
    public void testPostgreSQLPrepareThresholdLiteralOverride() {
        executeStatement("CREATE INDEX IF NOT EXISTS idx_task_status ON task (status)");
        executeStatement("VACUUM ANALYZE");

        doInJDBC(connection -> {
            executeStatement(connection, """
                SET auto_explain.log_min_duration TO 0;
                """);
            for (int i = 0; i < 100; i++) {
                selectByStatus(connection, Task.Status.DONE);
            }
            selectByStatusLiteral(connection, Task.Status.TO_DO);
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
        }
    }

    private List<String> selectByStatus(Connection connection, Task.Status status) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement("""
            SELECT *
            FROM task
            WHERE status = ?
            """)) {
            statement.setObject(1, PostgreSQLQueries.toEnum(status, "task_status"), Types.OTHER);
            ResultSet resultSet = statement.executeQuery();

            List<String> result = new ArrayList<>();
            while (resultSet.next()) {
                result.add(resultSet.getString(1));
            }
            return result;
        }
    }

    private List<String> selectByStatusLiteral(Connection connection, Task.Status status) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement(String.format("""
            SELECT *
            FROM task
            WHERE status = '%s'
            """, status.name()))) {

            ResultSet resultSet = statement.executeQuery();

            List<String> result = new ArrayList<>();
            while (resultSet.next()) {
                result.add(resultSet.getString(1));
            }
            return result;
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
