package com.vladmihalcea.hpjp.jdbc.index;

import com.microsoft.sqlserver.jdbc.SQLServerException;
import com.vladmihalcea.hpjp.util.AbstractTest;
import com.vladmihalcea.hpjp.util.providers.Database;
import jakarta.persistence.*;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.sql.*;
import java.time.LocalDateTime;
import java.time.Month;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.fail;

/**
 * SQLServerPushDownUnionAllTest - Test SQL Server index push down predicates optimization.
 *
 * @author Vlad Mihalcea
 */
public class SQLServerPushDownUnionAllTest extends AbstractTest {

    @Override
    protected Class<?>[] entities() {
        return new Class[] {
            Task.class,
            ApplicationTask.class,
            SystemTask.class
        };
    }

    private boolean initialize;

    @Override
    protected void beforeInit() {
        Integer taskCount = selectColumn("""
            select
            	(select count(*) from Tasks) +
            	(select count(*) from ApplicationTasks)  +
            	(select count(*) from SystemTasks)
            """, Integer.class);
        initialize = (taskCount == null || taskCount != getTaskCount());
        if(initialize) {
            executeStatement("drop table ApplicationTasks");
            executeStatement("drop table SystemTasks");
            executeStatement("drop table Tasks");
            executeStatement("drop sequence Tasks_SEQ");
            executeStatement("create sequence Tasks_SEQ start with 1 increment by 50");
            executeStatement("create table ApplicationTasks (CreatedOn datetime2(7), Id bigint not null, UpdatedOn datetime2(7), Name varchar(50), ApplicationName varchar(255), CategoryId bigint not null, primary key (Id))");
            executeStatement("create table SystemTasks (CreatedOn datetime2(7), Id bigint not null, UpdatedOn datetime2(7), Name varchar(50), OsVersion varchar(255), CategoryId bigint not null, primary key (Id))");
            executeStatement("create table Tasks (CreatedOn datetime2(7), Id bigint not null, UpdatedOn datetime2(7), Name varchar(50), CategoryId bigint not null, primary key (Id))");
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
        properties.put("hibernate.order_inserts", "true");
    }

    private LocalDateTime startTimestamp = LocalDateTime.of(2018, Month.APRIL, 1, 23, 59, 59);

    @Override
    protected void afterInit() {
        if(initialize) {
            long startNanos = System.nanoTime();
            doInJPA(entityManager -> {
                for (int i = 0; i < getTaskCount(); i++) {
                    double random = Math.random();
                    Task task = switch (i % 3) {
                        case 0 -> new Task();
                        case 1 -> new ApplicationTask().setApplicationName("HPJP");
                        case 2 -> new SystemTask().setOsVersion("Windows 11");
                        default -> throw new IllegalStateException("Unexpected value: " + i);
                    };
                    task.setCreatedOn(startTimestamp.plusHours(i).plusMinutes(23 * i).plusSeconds(45 * i));
                    if (random > 0.7) {
                        task.setUpdatedOn(task.createdOn.plusMinutes(6 * i));
                    }
                    task.setCategoryId(BigDecimal.valueOf(random).multiply(BigDecimal.valueOf(100)).longValue());
                    entityManager.persist(task);
                    if(i > 0 && i % getBatchSize() == 0) {
                        entityManager.flush();
                    }
                }
            });
            LOGGER.info("{}.testInsert took {} ms",
                getClass().getSimpleName(),
                TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - startNanos));
        }
    }

    @Test
    public void testTwoTimestampColumnIndexesInitialQuery() {
        executeStatement("DROP INDEX IF EXISTS TASKS_CREATED_ON ON Tasks");
        executeStatement("DROP INDEX IF EXISTS TASKS_UPDATED_ON ON Tasks");
        executeStatement("""
            CREATE NONCLUSTERED INDEX TASKS_CREATED_ON
            ON Tasks (CreatedOn)
            INCLUDE (CategoryId)
            """);
        executeStatement("""
            CREATE NONCLUSTERED INDEX TASKS_UPDATED_ON
            ON Tasks (UpdatedOn)
            INCLUDE (CategoryId)
            WHERE UpdatedOn IS NOT NULL
            """);

        executeStatement("DROP INDEX IF EXISTS APPLICATION_TASKS_CREATED_ON ON ApplicationTasks");
        executeStatement("DROP INDEX IF EXISTS APPLICATION_TASKS_UPDATED_ON ON ApplicationTasks");
        executeStatement("""
            CREATE NONCLUSTERED INDEX APPLICATION_TASKS_CREATED_ON
            ON ApplicationTasks (CreatedOn)
            INCLUDE (CategoryId)
            """);
        executeStatement("""
            CREATE NONCLUSTERED INDEX APPLICATION_TASKS_UPDATED_ON
            ON ApplicationTasks (UpdatedOn)
            INCLUDE (CategoryId)
            WHERE UpdatedOn IS NOT NULL
            """);

        executeStatement("DROP INDEX IF EXISTS SYSTEM_TASKS_CREATED_ON ON SystemTasks");
        executeStatement("DROP INDEX IF EXISTS SYSTEM_TASKS_UPDATED_ON ON SystemTasks");
        executeStatement("""
            CREATE NONCLUSTERED INDEX SYSTEM_TASKS_CREATED_ON
            ON SystemTasks (CreatedOn)
            INCLUDE (CategoryId)
            """);
        executeStatement("""
            CREATE NONCLUSTERED INDEX SYSTEM_TASKS_UPDATED_ON
            ON SystemTasks (UpdatedOn)
            INCLUDE (CategoryId)
            WHERE UpdatedOn IS NOT NULL
            """);

        executeStatement("UPDATE STATISTICS Tasks WITH FULLSCAN");
        doInJPA(entityManager -> {
            printExecutionPlan("""
                select CategoryId
                from (
                    select CategoryId
                    from (
                        select
                            CategoryId,
                            Name,
                            ISNULL(UpdatedOn, CreatedOn) AS Since
                        from
                            Tasks
                        union all
                        select
                            CategoryId,
                            ApplicationName,
                            ISNULL(UpdatedOn, CreatedOn) AS Since
                        from
                            ApplicationTasks
                        union all
                        select
                            CategoryId,
                            OsVersion,
                            ISNULL(UpdatedOn, CreatedOn) AS Since
                        from
                            SystemTasks
                    ) t
                    where t.Since >= ? and t.Since < ?
                    group by CategoryId
                ) o
                """,
                (preparedStatement -> {
                    try {
                        int i = 1;
                        preparedStatement.setTimestamp(i++, Timestamp.valueOf(startTimestamp.plusDays(1)));
                        preparedStatement.setTimestamp(i++, Timestamp.valueOf(startTimestamp.plusDays(7)));
                    } catch (SQLException e) {
                        fail(e.getMessage());
                    }
                }));
        });
    }

    @Test
    public void testTwoTimestampColumnIndexesAlternativeQuery1() {
        executeStatement("DROP INDEX IF EXISTS TASKS_CREATED_ON ON Tasks");
        executeStatement("DROP INDEX IF EXISTS TASKS_UPDATED_ON ON Tasks");
        executeStatement("""
            CREATE NONCLUSTERED INDEX TASKS_CREATED_ON
            ON Tasks (CreatedOn)
            INCLUDE (CategoryId)
            """);
        executeStatement("""
            CREATE NONCLUSTERED INDEX TASKS_UPDATED_ON
            ON Tasks (UpdatedOn)
            INCLUDE (CategoryId)
            WHERE UpdatedOn IS NOT NULL
            """);
        
        executeStatement("DROP INDEX IF EXISTS APPLICATION_TASKS_CREATED_ON ON ApplicationTasks");
        executeStatement("DROP INDEX IF EXISTS APPLICATION_TASKS_UPDATED_ON ON ApplicationTasks");
        executeStatement("""
            CREATE NONCLUSTERED INDEX APPLICATION_TASKS_CREATED_ON
            ON ApplicationTasks (CreatedOn)
            INCLUDE (CategoryId)
            """);
        executeStatement("""
            CREATE NONCLUSTERED INDEX APPLICATION_TASKS_UPDATED_ON
            ON ApplicationTasks (UpdatedOn)
            INCLUDE (CategoryId)
            WHERE UpdatedOn IS NOT NULL
            """);
        
        executeStatement("DROP INDEX IF EXISTS SYSTEM_TASKS_CREATED_ON ON SystemTasks");
        executeStatement("DROP INDEX IF EXISTS SYSTEM_TASKS_UPDATED_ON ON SystemTasks");
        executeStatement("""
            CREATE NONCLUSTERED INDEX SYSTEM_TASKS_CREATED_ON
            ON SystemTasks (CreatedOn)
            INCLUDE (CategoryId)
            """);
        executeStatement("""
            CREATE NONCLUSTERED INDEX SYSTEM_TASKS_UPDATED_ON
            ON SystemTasks (UpdatedOn)
            INCLUDE (CategoryId)
            WHERE UpdatedOn IS NOT NULL
            """);
        
        executeStatement("UPDATE STATISTICS Tasks WITH FULLSCAN");
        doInJPA(entityManager -> {
            printExecutionPlan("""
                select distinct CategoryId
                from (
                    select
                        CategoryId,
                        CreatedOn,
                        UpdatedOn
                    from
                        Tasks
                    union all
                    select
                        CategoryId,
                        CreatedOn,
                        UpdatedOn
                    from
                        ApplicationTasks
                    union all
                    select
                        CategoryId,
                        CreatedOn,
                        UpdatedOn
                    from
                        SystemTasks
                ) t
                where
                    ((t.UpdatedOn IS NOT NULL and t.UpdatedOn >= ?) or (t.UpdatedOn IS NULL and t.CreatedOn >= ?))
                and ((t.UpdatedOn IS NOT NULL and t.UpdatedOn < ?) or (t.UpdatedOn IS NULL and t.CreatedOn < ?))
                """,
                (preparedStatement -> {
                    try {
                        int i = 1;
                        preparedStatement.setTimestamp(i++, Timestamp.valueOf(startTimestamp.plusDays(1)));
                        preparedStatement.setTimestamp(i++, Timestamp.valueOf(startTimestamp.plusDays(1)));
                        preparedStatement.setTimestamp(i++, Timestamp.valueOf(startTimestamp.plusDays(7)));
                        preparedStatement.setTimestamp(i++, Timestamp.valueOf(startTimestamp.plusDays(7)));
                    } catch (SQLException e) {
                        fail(e.getMessage());
                    }
                }));
        });
    }

    @Test
    public void testTwoTimestampColumnIndexesAlternativeQuery12() {
        executeStatement("DROP INDEX IF EXISTS TASKS_CREATED_ON ON Tasks");
        executeStatement("DROP INDEX IF EXISTS TASKS_UPDATED_ON ON Tasks");
        executeStatement("""
            CREATE NONCLUSTERED INDEX TASKS_CREATED_ON
            ON Tasks (CreatedOn)
            INCLUDE (CategoryId)
            """);
        executeStatement("""
            CREATE NONCLUSTERED INDEX TASKS_UPDATED_ON
            ON Tasks (UpdatedOn)
            INCLUDE (CategoryId)
            WHERE UpdatedOn IS NOT NULL
            """);

        executeStatement("DROP INDEX IF EXISTS APPLICATION_TASKS_CREATED_ON ON ApplicationTasks");
        executeStatement("DROP INDEX IF EXISTS APPLICATION_TASKS_UPDATED_ON ON ApplicationTasks");
        executeStatement("""
            CREATE NONCLUSTERED INDEX APPLICATION_TASKS_CREATED_ON
            ON ApplicationTasks (CreatedOn)
            INCLUDE (CategoryId)
            """);
        executeStatement("""
            CREATE NONCLUSTERED INDEX APPLICATION_TASKS_UPDATED_ON
            ON ApplicationTasks (UpdatedOn)
            INCLUDE (CategoryId)
            WHERE UpdatedOn IS NOT NULL
            """);

        executeStatement("DROP INDEX IF EXISTS SYSTEM_TASKS_CREATED_ON ON SystemTasks");
        executeStatement("DROP INDEX IF EXISTS SYSTEM_TASKS_UPDATED_ON ON SystemTasks");
        executeStatement("""
            CREATE NONCLUSTERED INDEX SYSTEM_TASKS_CREATED_ON
            ON SystemTasks (CreatedOn)
            INCLUDE (CategoryId)
            """);
        executeStatement("""
            CREATE NONCLUSTERED INDEX SYSTEM_TASKS_UPDATED_ON
            ON SystemTasks (UpdatedOn)
            INCLUDE (CategoryId)
            WHERE UpdatedOn IS NOT NULL
            """);

        executeStatement("UPDATE STATISTICS Tasks WITH FULLSCAN");
        doInJPA(entityManager -> {
            printExecutionPlan("""
                select distinct CategoryId
                from (
                    select
                        CategoryId,
                        CreatedOn,
                        UpdatedOn
                    from
                        Tasks
                    union all
                    select
                        CategoryId,
                        CreatedOn,
                        UpdatedOn
                    from
                        ApplicationTasks
                    union all
                    select
                        CategoryId,
                        CreatedOn,
                        UpdatedOn
                    from
                        SystemTasks
                ) t
                where
                    (t.UpdatedOn IS NOT NULL and (t.UpdatedOn >= ? and t.UpdatedOn < ?))
                 or (t.UpdatedOn IS NULL and (t.CreatedOn >= ? and t.CreatedOn < ?))
                """,
                (preparedStatement -> {
                    try {
                        int i = 1;
                        preparedStatement.setTimestamp(i++, Timestamp.valueOf(startTimestamp.plusDays(1)));
                        preparedStatement.setTimestamp(i++, Timestamp.valueOf(startTimestamp.plusDays(7)));
                        preparedStatement.setTimestamp(i++, Timestamp.valueOf(startTimestamp.plusDays(1)));
                        preparedStatement.setTimestamp(i++, Timestamp.valueOf(startTimestamp.plusDays(7)));
                    } catch (SQLException e) {
                        fail(e.getMessage());
                    }
                }));
        });
    }

    @Test
    public void testTwoTimestampColumnIndexesAlternativeQuery13() {
        executeStatement("DROP INDEX IF EXISTS TASKS_CREATED_ON ON Tasks");
        executeStatement("DROP INDEX IF EXISTS TASKS_UPDATED_ON ON Tasks");
        executeStatement("""
            CREATE NONCLUSTERED INDEX TASKS_CREATED_ON
            ON Tasks (CreatedOn)
            INCLUDE (CategoryId)
            WHERE UpdatedOn IS NULL
            """);
        executeStatement("""
            CREATE NONCLUSTERED INDEX TASKS_UPDATED_ON
            ON Tasks (UpdatedOn)
            INCLUDE (CategoryId)
            WHERE UpdatedOn IS NOT NULL
            """);

        executeStatement("DROP INDEX IF EXISTS APPLICATION_TASKS_CREATED_ON ON ApplicationTasks");
        executeStatement("DROP INDEX IF EXISTS APPLICATION_TASKS_UPDATED_ON ON ApplicationTasks");
        executeStatement("""
            CREATE NONCLUSTERED INDEX APPLICATION_TASKS_CREATED_ON
            ON ApplicationTasks (CreatedOn)
            INCLUDE (CategoryId)
            """);
        executeStatement("""
            CREATE NONCLUSTERED INDEX APPLICATION_TASKS_UPDATED_ON
            ON ApplicationTasks (UpdatedOn)
            INCLUDE (CategoryId)
            WHERE UpdatedOn IS NOT NULL
            """);

        executeStatement("DROP INDEX IF EXISTS SYSTEM_TASKS_CREATED_ON ON SystemTasks");
        executeStatement("DROP INDEX IF EXISTS SYSTEM_TASKS_UPDATED_ON ON SystemTasks");
        executeStatement("""
            CREATE NONCLUSTERED INDEX SYSTEM_TASKS_CREATED_ON
            ON SystemTasks (CreatedOn)
            INCLUDE (CategoryId)
            """);
        executeStatement("""
            CREATE NONCLUSTERED INDEX SYSTEM_TASKS_UPDATED_ON
            ON SystemTasks (UpdatedOn)
            INCLUDE (CategoryId)
            WHERE UpdatedOn IS NOT NULL
            """);

        executeStatement("UPDATE STATISTICS Tasks WITH FULLSCAN");
        doInJPA(entityManager -> {
            printExecutionPlan("""
                select distinct CategoryId
                from (
                    select
                        CategoryId,
                        CreatedOn,
                        UpdatedOn
                    from
                        Tasks
                    union all
                    select
                        CategoryId,
                        CreatedOn,
                        UpdatedOn
                    from
                        ApplicationTasks
                    union all
                    select
                        CategoryId,
                        CreatedOn,
                        UpdatedOn
                    from
                        SystemTasks
                ) t
                where
                    (t.UpdatedOn IS NOT NULL and (t.UpdatedOn >= ? and t.UpdatedOn < ?))
                 or (t.UpdatedOn IS NULL and (t.CreatedOn >= ? and t.CreatedOn < ?))
                """,
                (preparedStatement -> {
                    try {
                        int i = 1;
                        preparedStatement.setTimestamp(i++, Timestamp.valueOf(startTimestamp.plusDays(1)));
                        preparedStatement.setTimestamp(i++, Timestamp.valueOf(startTimestamp.plusDays(7)));
                        preparedStatement.setTimestamp(i++, Timestamp.valueOf(startTimestamp.plusDays(1)));
                        preparedStatement.setTimestamp(i++, Timestamp.valueOf(startTimestamp.plusDays(7)));
                    } catch (SQLException e) {
                        fail(e.getMessage());
                    }
                }));
        });
    }

    @Test
    public void testTwoTimestampColumnIndexesAlternativeQuery14() {
        executeStatement("DROP INDEX IF EXISTS TASKS_CREATED_ON ON Tasks");
        executeStatement("DROP INDEX IF EXISTS TASKS_UPDATED_ON ON Tasks");
        executeStatement("""
            CREATE NONCLUSTERED INDEX TASKS_CREATED_ON
            ON Tasks (CreatedOn)
            INCLUDE (CategoryId, UpdatedOn)
            """);
        executeStatement("""
            CREATE NONCLUSTERED INDEX TASKS_UPDATED_ON
            ON Tasks (UpdatedOn)
            INCLUDE (CategoryId)
            WHERE UpdatedOn IS NOT NULL
            """);

        executeStatement("DROP INDEX IF EXISTS APPLICATION_TASKS_CREATED_ON ON ApplicationTasks");
        executeStatement("DROP INDEX IF EXISTS APPLICATION_TASKS_UPDATED_ON ON ApplicationTasks");
        executeStatement("""
            CREATE NONCLUSTERED INDEX APPLICATION_TASKS_CREATED_ON
            ON ApplicationTasks (CreatedOn)
            INCLUDE (CategoryId, UpdatedOn)
            """);
        executeStatement("""
            CREATE NONCLUSTERED INDEX APPLICATION_TASKS_UPDATED_ON
            ON ApplicationTasks (UpdatedOn)
            INCLUDE (CategoryId)
            WHERE UpdatedOn IS NOT NULL
            """);

        executeStatement("DROP INDEX IF EXISTS SYSTEM_TASKS_CREATED_ON ON SystemTasks");
        executeStatement("DROP INDEX IF EXISTS SYSTEM_TASKS_UPDATED_ON ON SystemTasks");
        executeStatement("""
            CREATE NONCLUSTERED INDEX SYSTEM_TASKS_CREATED_ON
            ON SystemTasks (CreatedOn)
            INCLUDE (CategoryId, UpdatedOn)
            """);
        executeStatement("""
            CREATE NONCLUSTERED INDEX SYSTEM_TASKS_UPDATED_ON
            ON SystemTasks (UpdatedOn)
            INCLUDE (CategoryId)
            WHERE UpdatedOn IS NOT NULL
            """);

        executeStatement("UPDATE STATISTICS Tasks WITH FULLSCAN");
        doInJPA(entityManager -> {
            printExecutionPlan("""
                select distinct CategoryId
                from (
                    select
                        CategoryId,
                        CreatedOn,
                        UpdatedOn
                    from
                        Tasks
                    union all
                    select
                        CategoryId,
                        CreatedOn,
                        UpdatedOn
                    from
                        ApplicationTasks
                    union all
                    select
                        CategoryId,
                        CreatedOn,
                        UpdatedOn
                    from
                        SystemTasks
                ) t
                where
                    (t.UpdatedOn IS NOT NULL and (t.UpdatedOn >= ? and t.UpdatedOn < ?))
                 or (t.UpdatedOn IS NULL and (t.CreatedOn >= ? and t.CreatedOn < ?))
                """,
                (preparedStatement -> {
                    try {
                        int i = 1;
                        preparedStatement.setTimestamp(i++, Timestamp.valueOf(startTimestamp.plusDays(1)));
                        preparedStatement.setTimestamp(i++, Timestamp.valueOf(startTimestamp.plusDays(7)));
                        preparedStatement.setTimestamp(i++, Timestamp.valueOf(startTimestamp.plusDays(1)));
                        preparedStatement.setTimestamp(i++, Timestamp.valueOf(startTimestamp.plusDays(7)));
                    } catch (SQLException e) {
                        fail(e.getMessage());
                    }
                }));
        });
    }

    @Test
    public void testTwoTimestampColumnIndexesAlternativeQuery2() {
        executeStatement("DROP INDEX IF EXISTS TASKS_CREATED_ON ON Tasks");
        executeStatement("DROP INDEX IF EXISTS TASKS_UPDATED_ON ON Tasks");
        executeStatement("""
            CREATE NONCLUSTERED INDEX TASKS_CREATED_ON
            ON Tasks (CreatedOn)
            INCLUDE (CategoryId)
            """);
        executeStatement("""
            CREATE NONCLUSTERED INDEX TASKS_UPDATED_ON
            ON Tasks (UpdatedOn)
            INCLUDE (CategoryId)
            WHERE UpdatedOn IS NOT NULL
            """);

        executeStatement("DROP INDEX IF EXISTS APPLICATION_TASKS_CREATED_ON ON ApplicationTasks");
        executeStatement("DROP INDEX IF EXISTS APPLICATION_TASKS_UPDATED_ON ON ApplicationTasks");
        executeStatement("""
            CREATE NONCLUSTERED INDEX APPLICATION_TASKS_CREATED_ON
            ON ApplicationTasks (CreatedOn)
            INCLUDE (CategoryId)
            """);
        executeStatement("""
            CREATE NONCLUSTERED INDEX APPLICATION_TASKS_UPDATED_ON
            ON ApplicationTasks (UpdatedOn)
            INCLUDE (CategoryId)
            WHERE UpdatedOn IS NOT NULL
            """);

        executeStatement("DROP INDEX IF EXISTS SYSTEM_TASKS_CREATED_ON ON SystemTasks");
        executeStatement("DROP INDEX IF EXISTS SYSTEM_TASKS_UPDATED_ON ON SystemTasks");
        executeStatement("""
            CREATE NONCLUSTERED INDEX SYSTEM_TASKS_CREATED_ON
            ON SystemTasks (CreatedOn)
            INCLUDE (CategoryId)
            """);
        executeStatement("""
            CREATE NONCLUSTERED INDEX SYSTEM_TASKS_UPDATED_ON
            ON SystemTasks (UpdatedOn)
            INCLUDE (CategoryId)
            WHERE UpdatedOn IS NOT NULL
            """);

        executeStatement("UPDATE STATISTICS Tasks WITH FULLSCAN");
        doInJPA(entityManager -> {
            printExecutionPlan("""
                select CategoryId
                from (
                    select
                        CategoryId
                    from
                        Tasks
                    where
                        ((UpdatedOn IS NOT NULL and UpdatedOn >= ?) or (UpdatedOn IS NULL and CreatedOn >= ?))
                    and ((UpdatedOn IS NOT NULL and UpdatedOn < ?) or (UpdatedOn IS NULL and CreatedOn < ?))
                    group by CategoryId
                    union
                    select
                        CategoryId
                    from
                        ApplicationTasks
                    where
                        ((UpdatedOn IS NOT NULL and UpdatedOn >= ?) or (UpdatedOn IS NULL and CreatedOn >= ?))
                    and ((UpdatedOn IS NOT NULL and UpdatedOn < ?) or (UpdatedOn IS NULL and CreatedOn < ?))
                    group by CategoryId
                    union
                    select
                        CategoryId
                    from
                        SystemTasks
                    where
                        ((UpdatedOn IS NOT NULL and UpdatedOn >= ?) or (UpdatedOn IS NULL and CreatedOn >= ?))
                    and ((UpdatedOn IS NOT NULL and UpdatedOn < ?) or (UpdatedOn IS NULL and CreatedOn < ?))
                    group by CategoryId
                ) t
                
                """,
                (preparedStatement -> {
                    try {
                        int i = 1;
                        preparedStatement.setTimestamp(i++, Timestamp.valueOf(startTimestamp.plusDays(1)));
                        preparedStatement.setTimestamp(i++, Timestamp.valueOf(startTimestamp.plusDays(1)));
                        preparedStatement.setTimestamp(i++, Timestamp.valueOf(startTimestamp.plusDays(7)));
                        preparedStatement.setTimestamp(i++, Timestamp.valueOf(startTimestamp.plusDays(7)));
                        preparedStatement.setTimestamp(i++, Timestamp.valueOf(startTimestamp));
                        preparedStatement.setTimestamp(i++, Timestamp.valueOf(startTimestamp));
                        preparedStatement.setTimestamp(i++, Timestamp.valueOf(startTimestamp.plusDays(7)));
                        preparedStatement.setTimestamp(i++, Timestamp.valueOf(startTimestamp.plusDays(7)));
                        preparedStatement.setTimestamp(i++, Timestamp.valueOf(startTimestamp));
                        preparedStatement.setTimestamp(i++, Timestamp.valueOf(startTimestamp));
                        preparedStatement.setTimestamp(i++, Timestamp.valueOf(startTimestamp.plusDays(7)));
                        preparedStatement.setTimestamp(i++, Timestamp.valueOf(startTimestamp.plusDays(7)));
                    } catch (SQLException e) {
                        fail(e.getMessage());
                    }
                }));
        });
    }

    private void printExecutionPlan(String query, Consumer<PreparedStatement> bind) {
        //Check Execution Plan
        doInJDBC(connection -> {
            List<Map<String, String>> planLines = new ArrayList<>();

            try (PreparedStatement preparedStatement = connection.prepareStatement(query);
                Statement statement = connection.createStatement();) {
                statement.executeUpdate(
                    "SET STATISTICS IO, TIME, PROFILE ON"
                );

                bind.accept(preparedStatement);

                //warming up
                for (int i = 0; i < 100; i++) {
                    preparedStatement.executeQuery();
                }

                boolean moreResultSets = preparedStatement.execute();

                while (moreResultSets) {
                    planLines.addAll(parseResultSet(preparedStatement.getResultSet()));

                    moreResultSets = preparedStatement.getMoreResults();
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
    }

    protected int getTaskCount() {
        return 300 * 1000;
    }

    protected int getBatchSize() {
        return 1000;
    }

    @Entity(name = "Task")
    @Table(name = "Tasks")
    @Inheritance(strategy = InheritanceType.TABLE_PER_CLASS)
    public static class Task {

        @Id
        @GeneratedValue(strategy = GenerationType.SEQUENCE)
        @Column(name = "Id")
        private Long id;

        @Column(name = "CategoryId")
        private Long categoryId;

        @Column(name = "Name", length = 50)
        private String name;

        @Column(name = "CreatedOn")
        private LocalDateTime createdOn;

        @Column(name = "UpdatedOn")
        private LocalDateTime updatedOn;

        public Long getId() {
            return id;
        }

        public Task setId(Long id) {
            this.id = id;
            return this;
        }

        public Long getCategoryId() {
            return categoryId;
        }

        public Task setCategoryId(Long categoryId) {
            this.categoryId = categoryId;
            return this;
        }

        public String getName() {
            return name;
        }

        public Task setName(String name) {
            this.name = name;
            return this;
        }

        public LocalDateTime getCreatedOn() {
            return createdOn;
        }

        public Task setCreatedOn(LocalDateTime createdOn) {
            this.createdOn = createdOn;
            return this;
        }

        public LocalDateTime getUpdatedOn() {
            return updatedOn;
        }

        public Task setUpdatedOn(LocalDateTime updatedOn) {
            this.updatedOn = updatedOn;
            return this;
        }
    }

    @Entity(name = "ApplicationTask")
    @Table(name = "ApplicationTasks")
    public static class ApplicationTask extends Task {

        @Column(name = "ApplicationName")
        private String applicationName;

        public String getApplicationName() {
            return applicationName;
        }

        public ApplicationTask setApplicationName(String applicationName) {
            this.applicationName = applicationName;
            return this;
        }
    }

    @Entity(name = "SystemTask")
    @Table(name = "SystemTasks")
    public static class SystemTask extends Task {

        @Column(name = "OsVersion")
        private String osVersion;

        public String getOsVersion() {
            return osVersion;
        }

        public SystemTask setOsVersion(String osVersion) {
            this.osVersion = osVersion;
            return this;
        }
    }
}
