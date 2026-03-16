package com.vladmihalcea.hpjp.spring.transaction.jta.narayana.config;

import jakarta.transaction.SystemException;
import org.camunda.bpm.engine.*;
import org.camunda.bpm.engine.spring.ProcessEngineFactoryBean;
import org.camunda.bpm.engine.spring.SpringProcessEngineConfiguration;
import org.springframework.context.annotation.*;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.transaction.annotation.EnableTransactionManagement;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

/**
 * Spring configuration for Camunda BPM with Narayana JTA and SQL Server.
 * <p>
 * This demonstrates how to integrate Camunda Process Engine with JTA transactions
 * managed by Narayana, using SQL Server as the database via XA DataSource.
 *
 * @author Vlad Mihalcea
 */
@Configuration
@PropertySource({"/META-INF/jta-sqlserver.properties"})
@ComponentScan(basePackages = {
    "com.vladmihalcea.hpjp.spring.transaction.jta.dao",
    "com.vladmihalcea.hpjp.spring.transaction.jta.service",
    "com.vladmihalcea.hpjp.spring.transaction.jta.camunda",
})
@EnableTransactionManagement
@EnableAspectJAutoProxy
public class CamundaNarayanaJTATransactionManagerSQLServerConfiguration extends NarayanaJTATransactionManagerSQLServerConfiguration {

    // Camunda Process Engine Configuration

    @Bean
    public SpringProcessEngineConfiguration processEngineConfiguration() throws SystemException {
        dropCamundaTables(actualDataSource());

        SpringProcessEngineConfiguration config = new SpringProcessEngineConfiguration();
        config.setDataSource(actualDataSource());
        config.setTransactionManager(transactionManager());
        config.setTransactionsExternallyManaged(true);
        config.setDatabaseSchemaUpdate("create-drop");
        config.setJobExecutorActivate(false);
        config.setHistory("audit");
        config.setDeploymentResources(
            new Resource[]{
                new ClassPathResource("bpmn/forum-post-process.bpmn")
            }
        );
        return config;
    }

    @Override
    protected void initDatabase(DataSource dataSource) {
        try (Connection connection = dataSource.getConnection();
             Statement statement = connection.createStatement()) {
            statement.executeLargeUpdate("ALTER DATABASE [high_performance_java_persistence] SET READ_COMMITTED_SNAPSHOT OFF");
        } catch (SQLException e) {
            LOGGER.error("Statement failed", e);
        }
    }

    private void dropCamundaTables(DataSource dataSource) {
        try (Connection connection = dataSource.getConnection()) {
            List<String> tables = new ArrayList<>();
            try (ResultSet rs = connection.getMetaData().getTables(
                null, null, "ACT_%", new String[]{"TABLE"})) {
                while (rs.next()) {
                    tables.add(rs.getString("TABLE_NAME"));
                }
            }
            if (!tables.isEmpty()) {
                try (Statement stmt = connection.createStatement()) {
                    // Drop foreign keys first
                    try (ResultSet rs = stmt.executeQuery("""
                        SELECT
                          fk.name AS fk_name,
                          t.name AS table_name
                        FROM sys.foreign_keys fk
                        JOIN sys.tables t ON fk.parent_object_id = t.object_id
                        WHERE t.name LIKE 'ACT_%'
                        """
                    )) {
                        List<String> dropFkStatements = new ArrayList<>();
                        while (rs.next()) {
                            dropFkStatements.add(String.format(
                                "ALTER TABLE [%s] DROP CONSTRAINT [%s]",
                                rs.getString("table_name"),
                                rs.getString("fk_name")
                            ));
                        }
                        for (String sql : dropFkStatements) {
                            stmt.execute(sql);
                        }
                    }
                    // Now drop the tables
                    for (String table : tables) {
                        stmt.execute("DROP TABLE IF EXISTS [" + table + "]");
                    }
                }
            }
        } catch (SQLException e) {
            // Ignore - tables might not exist
        }
    }

    @Bean
    public ProcessEngineFactoryBean processEngine() throws SystemException {
        ProcessEngineFactoryBean factoryBean = new ProcessEngineFactoryBean();
        factoryBean.setProcessEngineConfiguration(processEngineConfiguration());
        return factoryBean;
    }

    @Bean
    public RepositoryService repositoryService(ProcessEngine processEngine) {
        return processEngine.getRepositoryService();
    }

    @Bean
    public RuntimeService runtimeService(ProcessEngine processEngine) {
        return processEngine.getRuntimeService();
    }

    @Bean
    public TaskService taskService(ProcessEngine processEngine) {
        return processEngine.getTaskService();
    }

    @Bean
    public HistoryService historyService(ProcessEngine processEngine) {
        return processEngine.getHistoryService();
    }
}
