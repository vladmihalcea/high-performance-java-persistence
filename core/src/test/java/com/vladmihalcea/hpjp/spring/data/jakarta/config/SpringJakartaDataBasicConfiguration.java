package com.vladmihalcea.hpjp.spring.data.jakarta.config;

import com.vladmihalcea.hpjp.hibernate.forum.dto.PostDTO;
import com.vladmihalcea.hpjp.spring.data.jakarta.domain.Post;
import com.vladmihalcea.hpjp.spring.data.jakarta.repository.PostCommentRepository;
import com.vladmihalcea.hpjp.spring.data.jakarta.repository.PostRepository;
import com.vladmihalcea.hpjp.spring.data.jakarta.repository.StatelessSessionInvocationHandler;
import com.vladmihalcea.hpjp.util.DataSourceProxyType;
import com.vladmihalcea.hpjp.util.ReflectionUtils;
import com.vladmihalcea.hpjp.util.logging.InlineQueryLogEntryCreator;
import com.vladmihalcea.hpjp.util.providers.DataSourceProvider;
import com.vladmihalcea.hpjp.util.providers.Database;
import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import io.hypersistence.utils.hibernate.type.util.ClassImportIntegrator;
import jakarta.persistence.EntityManagerFactory;
import net.ttddyy.dsproxy.listener.ChainListener;
import net.ttddyy.dsproxy.listener.DataSourceQueryCountListener;
import net.ttddyy.dsproxy.listener.logging.SLF4JQueryLoggingListener;
import net.ttddyy.dsproxy.support.ProxyDataSourceBuilder;
import org.hibernate.StatelessSession;
import org.hibernate.jpa.HibernatePersistenceProvider;
import org.hibernate.jpa.boot.spi.IntegratorProvider;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.EnableAspectJAutoProxy;
import org.springframework.orm.jpa.JpaTransactionManager;
import org.springframework.orm.jpa.JpaVendorAdapter;
import org.springframework.orm.jpa.LocalContainerEntityManagerFactoryBean;
import org.springframework.orm.jpa.vendor.HibernateJpaVendorAdapter;
import org.springframework.transaction.annotation.EnableTransactionManagement;
import org.springframework.transaction.support.TransactionTemplate;

import javax.sql.DataSource;
import java.lang.reflect.Proxy;
import java.util.Arrays;
import java.util.Collections;
import java.util.Properties;

/**
 * @author Vlad Mihalcea
 */
@Configuration
@EnableTransactionManagement
@EnableAspectJAutoProxy
@ComponentScan(
    basePackages = {
        "com.vladmihalcea.hpjp.spring.data.jakarta",
    }
)
public class SpringJakartaDataBasicConfiguration {

    public static final String DATA_SOURCE_PROXY_NAME = DataSourceProxyType.DATA_SOURCE_PROXY.name();

    @Bean
    public Database database() {
        return Database.POSTGRESQL;
    }

    @Bean
    public DataSourceProvider dataSourceProvider() {
        return database().dataSourceProvider();
    }

    @Bean
    public HikariDataSource actualDataSource() {
        HikariConfig hikariConfig = new HikariConfig();
        hikariConfig.setAutoCommit(false);
        hikariConfig.setDataSource(dataSourceProvider().dataSource());
        return new HikariDataSource(hikariConfig);
    }

    @Bean
    public DataSource dataSource() {
        ChainListener listener = new ChainListener();
        SLF4JQueryLoggingListener loggingListener = new SLF4JQueryLoggingListener();
        loggingListener.setQueryLogEntryCreator(new InlineQueryLogEntryCreator());
        listener.addListener(loggingListener);
        listener.addListener(new DataSourceQueryCountListener());
        return ProxyDataSourceBuilder
            .create(actualDataSource())
            .name(DATA_SOURCE_PROXY_NAME)
            .listener(listener)
            .build();
    }

    @Bean
    public LocalContainerEntityManagerFactoryBean entityManagerFactory() {
        LocalContainerEntityManagerFactoryBean entityManagerFactoryBean = new LocalContainerEntityManagerFactoryBean();
        entityManagerFactoryBean.setPersistenceUnitName(getClass().getSimpleName());
        entityManagerFactoryBean.setPersistenceProvider(new HibernatePersistenceProvider());
        entityManagerFactoryBean.setDataSource(dataSource());
        entityManagerFactoryBean.setPackagesToScan(packagesToScan());

        JpaVendorAdapter vendorAdapter = new HibernateJpaVendorAdapter();
        entityManagerFactoryBean.setJpaVendorAdapter(vendorAdapter);
        entityManagerFactoryBean.setJpaProperties(properties());
        return entityManagerFactoryBean;
    }

    @Bean
    public JpaTransactionManager transactionManager(EntityManagerFactory entityManagerFactory) {
        JpaTransactionManager transactionManager = new JpaTransactionManager();
        transactionManager.setEntityManagerFactory(entityManagerFactory);
        return transactionManager;
    }

    @Bean
    public TransactionTemplate transactionTemplate(EntityManagerFactory entityManagerFactory) {
        return new TransactionTemplate(transactionManager(entityManagerFactory));
    }

    @Bean
    public StatelessSession statelessSession(EntityManagerFactory entityManagerFactory) {
        return (StatelessSession) Proxy.newProxyInstance(
            StatelessSession.class.getClassLoader(),
            new Class[]{StatelessSession.class},
            new StatelessSessionInvocationHandler(entityManagerFactory)
        );
    }

    @Bean
    public PostRepository postRepository(StatelessSession statelessSession) {
        return ReflectionUtils.newInstance(
            "com.vladmihalcea.hpjp.spring.data.jakarta.repository.PostRepository_",
            new Object[]{statelessSession},
            new Class[]{StatelessSession.class}
        );
    }

    @Bean
    public PostCommentRepository postCommentRepository(StatelessSession statelessSession) {
        return ReflectionUtils.newInstance(
            "com.vladmihalcea.hpjp.spring.data.jakarta.repository.PostCommentRepository_",
            new Object[]{statelessSession},
            new Class[]{StatelessSession.class}
        );
    }

    protected Properties properties() {
        Properties properties = new Properties();
        properties.setProperty("hibernate.hbm2ddl.auto", "create-drop");
        properties.put(
            "hibernate.integrator_provider",
            (IntegratorProvider) () -> Collections.singletonList(
                new ClassImportIntegrator(Arrays.asList(PostDTO.class))
            )
        );
        additionalProperties(properties);
        return properties;
    }

    protected String[] packagesToScan() {
        return new String[]{
            packageToScan()
        };
    }

    protected String packageToScan() {
        return Post.class.getPackageName();
    }

    protected void additionalProperties(Properties properties) {
        properties.put("hibernate.jdbc.batch_size", "100");
        properties.put("hibernate.order_inserts", "true");
    }
}
