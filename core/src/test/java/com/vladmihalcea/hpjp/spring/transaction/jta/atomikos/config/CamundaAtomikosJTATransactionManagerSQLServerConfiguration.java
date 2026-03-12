package com.vladmihalcea.hpjp.spring.transaction.jta.atomikos.config;

import com.atomikos.icatch.jta.UserTransactionManager;
import com.atomikos.jdbc.AtomikosDataSourceBean;
import com.microsoft.sqlserver.jdbc.SQLServerXADataSource;
import com.vladmihalcea.hpjp.hibernate.forum.dto.PostDTO;
import com.vladmihalcea.hpjp.hibernate.logging.LoggingStatementInspector;
import com.vladmihalcea.hpjp.util.DataSourceProxyType;
import com.vladmihalcea.hpjp.util.logging.InlineQueryLogEntryCreator;
import io.hypersistence.utils.hibernate.type.util.ClassImportIntegrator;
import jakarta.transaction.SystemException;
import net.ttddyy.dsproxy.listener.logging.SLF4JQueryLoggingListener;
import net.ttddyy.dsproxy.support.ProxyDataSourceBuilder;
import org.camunda.bpm.engine.ProcessEngine;
import org.camunda.bpm.engine.RepositoryService;
import org.camunda.bpm.engine.RuntimeService;
import org.camunda.bpm.engine.TaskService;
import org.camunda.bpm.engine.HistoryService;
import org.camunda.bpm.engine.spring.ProcessEngineFactoryBean;
import org.camunda.bpm.engine.spring.SpringProcessEngineConfiguration;
import org.hibernate.cfg.AvailableSettings;
import org.hibernate.engine.transaction.jta.platform.internal.AtomikosJtaPlatform;
import org.hibernate.jpa.boot.spi.IntegratorProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.*;
import org.springframework.context.support.PropertySourcesPlaceholderConfigurer;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.orm.jpa.JpaVendorAdapter;
import org.springframework.orm.jpa.LocalContainerEntityManagerFactoryBean;
import org.springframework.orm.jpa.vendor.HibernateJpaVendorAdapter;
import org.springframework.transaction.annotation.EnableTransactionManagement;
import org.springframework.transaction.jta.JtaTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Properties;

/**
 * Spring configuration for Camunda BPM with Atomikos JTA and SQL Server.
 * <p>
 * This demonstrates how to integrate Camunda Process Engine with JTA transactions
 * managed by Atomikos, using SQL Server as the database via XA DataSource.
 *
 * @author Vlad Mihalcea
 */
@Configuration
@PropertySource({"/META-INF/jta-sqlserver.properties"})
@ComponentScan(basePackages = {
    "com.vladmihalcea.hpjp.spring.transaction.jta.atomikos.dao",
    "com.vladmihalcea.hpjp.spring.transaction.jta.atomikos.service",
    "com.vladmihalcea.hpjp.spring.transaction.jta.atomikos.camunda",
})
@EnableTransactionManagement
@EnableAspectJAutoProxy
public class CamundaAtomikosJTATransactionManagerSQLServerConfiguration {

    public static final String DATA_SOURCE_PROXY_NAME = DataSourceProxyType.DATA_SOURCE_PROXY.name();

    @Value("${hibernate.dialect}")
    protected String hibernateDialect;

    @Value("${jdbc.username}")
    protected String jdbcUser;

    @Value("${jdbc.password}")
    protected String jdbcPassword;

    @Value("${jdbc.url}")
    protected String jdbcURL;

    @Bean
    public static PropertySourcesPlaceholderConfigurer properties() {
        return new PropertySourcesPlaceholderConfigurer();
    }

    @DependsOn("actualDataSource")
    public DataSource dataSource() {
        SLF4JQueryLoggingListener loggingListener = new SLF4JQueryLoggingListener();
        loggingListener.setQueryLogEntryCreator(new InlineQueryLogEntryCreator());
        return ProxyDataSourceBuilder
            .create(actualDataSource())
            .name(DATA_SOURCE_PROXY_NAME)
            .listener(loggingListener)
            .build();
    }

    @Bean(initMethod = "init", destroyMethod = "close")
    public AtomikosDataSourceBean actualDataSource() {
        AtomikosDataSourceBean dataSource = new AtomikosDataSourceBean();
        dataSource.setUniqueResourceName("SQLServer");
        SQLServerXADataSource xaDataSource = new SQLServerXADataSource();
        xaDataSource.setURL(jdbcURL);
        xaDataSource.setUser(jdbcUser);
        xaDataSource.setPassword(jdbcPassword);
        dataSource.setXaDataSource(xaDataSource);
        dataSource.setPoolSize(5);
        return dataSource;
    }

    @Bean
    public LocalContainerEntityManagerFactoryBean entityManagerFactory() {
        LocalContainerEntityManagerFactoryBean localContainerEntityManagerFactoryBean = new LocalContainerEntityManagerFactoryBean();
        localContainerEntityManagerFactoryBean.setPersistenceUnitName(getClass().getSimpleName());
        localContainerEntityManagerFactoryBean.setJtaDataSource(dataSource());
        localContainerEntityManagerFactoryBean.setPackagesToScan(packagesToScan());

        JpaVendorAdapter vendorAdapter = new HibernateJpaVendorAdapter();
        localContainerEntityManagerFactoryBean.setJpaVendorAdapter(vendorAdapter);
        localContainerEntityManagerFactoryBean.setJpaProperties(additionalProperties());
        return localContainerEntityManagerFactoryBean;
    }

    @Bean(initMethod = "init", destroyMethod = "close")
    public UserTransactionManager userTransactionManager() throws SystemException {
        UserTransactionManager userTransactionManager = new UserTransactionManager();
        userTransactionManager.setTransactionTimeout(300);
        userTransactionManager.setForceShutdown(true);
        return userTransactionManager;
    }

    @Bean
    public JtaTransactionManager transactionManager() throws SystemException {
        JtaTransactionManager jtaTransactionManager = new JtaTransactionManager();
        jtaTransactionManager.setTransactionManager(userTransactionManager());
        jtaTransactionManager.setUserTransaction(userTransactionManager());
        jtaTransactionManager.setAllowCustomIsolationLevels(true);
        return jtaTransactionManager;
    }

    @Bean
    public TransactionTemplate transactionTemplate() throws SystemException {
        return new TransactionTemplate(transactionManager());
    }

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

    protected Properties additionalProperties() {
        Properties properties = new Properties();
        properties.put(
            AvailableSettings.JTA_PLATFORM,
            AtomikosJtaPlatform.class
        );
        properties.setProperty("hibernate.hbm2ddl.auto", "create-drop");
        properties.put(
            "hibernate.session_factory.statement_inspector",
            new LoggingStatementInspector("com.vladmihalcea.hpjp.hibernate.transaction")
        );
        properties.put(
            "hibernate.integrator_provider",
            (IntegratorProvider) () -> Collections.singletonList(
                new ClassImportIntegrator(Arrays.asList(PostDTO.class))
            )
        );
        properties.put(AvailableSettings.DIALECT, hibernateDialect);

        return properties;
    }

    protected String[] packagesToScan() {
        return new String[]{
            "com.vladmihalcea.hpjp.hibernate.transaction.forum"
        };
    }
}

