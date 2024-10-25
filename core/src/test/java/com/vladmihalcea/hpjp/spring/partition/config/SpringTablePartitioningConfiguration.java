package com.vladmihalcea.hpjp.spring.partition.config;

import com.vladmihalcea.hpjp.spring.partition.domain.PartitionAware;
import com.vladmihalcea.hpjp.spring.partition.domain.User;
import com.vladmihalcea.hpjp.spring.partition.event.PartitionAwareEventListenerIntegrator;
import com.vladmihalcea.hpjp.spring.partition.util.UserContext;
import com.vladmihalcea.hpjp.util.DataSourceProxyType;
import com.vladmihalcea.hpjp.util.logging.InlineQueryLogEntryCreator;
import com.vladmihalcea.hpjp.util.providers.DataSourceProvider;
import com.vladmihalcea.hpjp.util.providers.Database;
import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import io.hypersistence.utils.spring.repository.BaseJpaRepositoryImpl;
import jakarta.persistence.EntityManagerFactory;
import net.ttddyy.dsproxy.listener.logging.SLF4JQueryLoggingListener;
import net.ttddyy.dsproxy.support.ProxyDataSourceBuilder;
import org.hibernate.Session;
import org.hibernate.jpa.HibernatePersistenceProvider;
import org.hibernate.jpa.boot.internal.EntityManagerFactoryBuilderImpl;
import org.hibernate.jpa.boot.spi.IntegratorProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.EnableAspectJAutoProxy;
import org.springframework.context.support.PropertySourcesPlaceholderConfigurer;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.orm.jpa.JpaTransactionManager;
import org.springframework.orm.jpa.JpaVendorAdapter;
import org.springframework.orm.jpa.LocalContainerEntityManagerFactoryBean;
import org.springframework.orm.jpa.vendor.HibernateJpaVendorAdapter;
import org.springframework.transaction.annotation.EnableTransactionManagement;
import org.springframework.transaction.support.TransactionTemplate;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.List;
import java.util.Properties;

/**
 *
 * @author Vlad Mihalcea
 */
@Configuration
@EnableTransactionManagement
@EnableAspectJAutoProxy
@EnableJpaRepositories(
    value = "com.vladmihalcea.hpjp.spring.partition.repository",
    repositoryBaseClass = BaseJpaRepositoryImpl.class
)
@ComponentScan(
    value = {
        "com.vladmihalcea.hpjp.spring.partition.service",
        "io.hypersistence.utils.spring.aop"
    }
)
public class SpringTablePartitioningConfiguration {

    public static final String DATA_SOURCE_PROXY_NAME = DataSourceProxyType.DATA_SOURCE_PROXY.name();

    @Bean
    public static PropertySourcesPlaceholderConfigurer properties() {
        return new PropertySourcesPlaceholderConfigurer();
    }

    @Bean
    public Database database() {
        return Database.POSTGRESQL;
    }

    @Bean
    public DataSourceProvider dataSourceProvider() {
        return database().dataSourceProvider();
    }

    public DataSource poolingDataSource() {
        HikariConfig hikariConfig = new HikariConfig();
        hikariConfig.setAutoCommit(false);
        hikariConfig.setDataSource(dataSourceProvider().dataSource());
        return new HikariDataSource(hikariConfig);
    }

    @Bean
    public DataSource dataSource() {
        SLF4JQueryLoggingListener loggingListener = new SLF4JQueryLoggingListener();
        loggingListener.setQueryLogEntryCreator(new InlineQueryLogEntryCreator());
        DataSource dataSource = ProxyDataSourceBuilder
                .create(poolingDataSource())
                .name(DATA_SOURCE_PROXY_NAME)
                .listener(loggingListener)
                .build();
        
        try(Connection connection = dataSource.getConnection();
            Statement statement = connection.createStatement()) {
            connection.setAutoCommit(true);
            createDatabaseSchema(statement);
        } catch (SQLException e) {
            throw new IllegalStateException(e);
        }
        return dataSource;
    }

    private void createDatabaseSchema(Statement statement) throws SQLException {
        executeStatement(statement, "DROP TABLE IF EXISTS posts cascade");
        executeStatement(statement, "DROP TABLE IF EXISTS users cascade");
        executeStatement(statement, "DROP SEQUENCE IF EXISTS posts_seq");
        executeStatement(statement, "DROP SEQUENCE IF EXISTS users_seq");

        executeStatement(statement, "CREATE SEQUENCE posts_seq START WITH 1 INCREMENT BY 50");
        executeStatement(statement, "CREATE SEQUENCE users_seq START WITH 1 INCREMENT BY 50");

        executeStatement(statement, """
            CREATE TABLE users (
                id bigint NOT NULL,
                first_name varchar(255),
                last_name varchar(255),
                registered_on timestamp(6),
                partition_key varchar(255),
                PRIMARY KEY (id, partition_key)
            ) PARTITION BY LIST (partition_key)
            """);
        executeStatement(statement, "CREATE TABLE users_asia PARTITION OF users FOR VALUES IN ('Asia')");
        executeStatement(statement, "CREATE TABLE users_africa PARTITION OF users FOR VALUES IN ('Africa')");
        executeStatement(statement, "CREATE TABLE users_north_america PARTITION OF users FOR VALUES IN ('North America')");
        executeStatement(statement, "CREATE TABLE users_south_america PARTITION OF users FOR VALUES IN ('South America')");
        executeStatement(statement, "CREATE TABLE users_europe PARTITION OF users FOR VALUES IN ('Europe')");
        executeStatement(statement, "CREATE TABLE users_australia PARTITION OF users FOR VALUES IN ('Australia')");

        executeStatement(statement, """
            CREATE TABLE posts (
                id bigint NOT NULL,
                title varchar(255),
                created_on timestamp(6),
                user_id bigint,
                partition_key varchar(255),
                PRIMARY KEY (id, partition_key)
            ) PARTITION BY LIST (partition_key)
            """);
        executeStatement(statement, "CREATE TABLE posts_asia PARTITION OF posts FOR VALUES IN ('Asia')");
        executeStatement(statement, "CREATE TABLE posts_africa PARTITION OF posts FOR VALUES IN ('Africa')");
        executeStatement(statement, "CREATE TABLE posts_north_america PARTITION OF posts FOR VALUES IN ('North America')");
        executeStatement(statement, "CREATE TABLE posts_south_america PARTITION OF posts FOR VALUES IN ('South America')");
        executeStatement(statement, "CREATE TABLE posts_europe PARTITION OF posts FOR VALUES IN ('Europe')");
        executeStatement(statement, "CREATE TABLE posts_australia PARTITION OF posts FOR VALUES IN ('Australia')");

        executeStatement(statement, """
            ALTER TABLE IF EXISTS posts
            ADD CONSTRAINT fk_posts_user_id FOREIGN KEY (user_id, partition_key) REFERENCES users
            """);
    }

    private void executeStatement(Statement statement, String sql) throws SQLException {
        statement.executeUpdate(sql);
    }

    @Bean
    public LocalContainerEntityManagerFactoryBean entityManagerFactory(
            @Autowired DataSource dataSource) {
        LocalContainerEntityManagerFactoryBean entityManagerFactoryBean = new LocalContainerEntityManagerFactoryBean();
        entityManagerFactoryBean.setPersistenceUnitName(getClass().getSimpleName());
        
        entityManagerFactoryBean.setDataSource(dataSource);
        entityManagerFactoryBean.setPackagesToScan(packagesToScan());

        JpaVendorAdapter vendorAdapter = new HibernateJpaVendorAdapter();
        entityManagerFactoryBean.setJpaVendorAdapter(vendorAdapter);
        entityManagerFactoryBean.setJpaProperties(additionalProperties());
        return entityManagerFactoryBean;
    }

    @Bean
    public JpaTransactionManager transactionManager(EntityManagerFactory entityManagerFactory){
        JpaTransactionManager transactionManager = new JpaTransactionManager();
        transactionManager.setEntityManagerFactory(entityManagerFactory);
        transactionManager.setEntityManagerInitializer(entityManager -> {
            User user = UserContext.getCurrent();
            if (user != null) {
                entityManager.unwrap(Session.class)
                    .enableFilter(PartitionAware.PARTITION_KEY)
                    .setParameter(PartitionAware.PARTITION_KEY, user.getPartitionKey());
            }
        });
        return transactionManager;
    }

    @Bean
    public TransactionTemplate transactionTemplate(EntityManagerFactory entityManagerFactory) {
        return new TransactionTemplate(transactionManager(entityManagerFactory));
    }

    @Bean
    public Integer partitionProcessingSize() {
        return 100;
    }

    protected Properties additionalProperties() {
        Properties properties = new Properties();
        properties.setProperty("hibernate.hbm2ddl.auto", "none");
        properties.setProperty("hibernate.jdbc.batch_size", partitionProcessingSize().toString());
        properties.setProperty("hibernate.order_inserts", "true");
        properties.setProperty("hibernate.order_updates", "true");
        properties.put(
            EntityManagerFactoryBuilderImpl.INTEGRATOR_PROVIDER,
            (IntegratorProvider) () -> List.of(
                PartitionAwareEventListenerIntegrator.INSTANCE
            )
        );
        return properties;
    }

    protected String[] packagesToScan() {
        return new String[]{
            "com.vladmihalcea.hpjp.spring.partition.domain"
        };
    }
}
