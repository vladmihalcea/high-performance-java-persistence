package com.vladmihalcea.hpjp.spring.transaction.jta.narayana.config;

import com.arjuna.ats.internal.jta.recovery.arjunacore.XARecoveryModule;
import com.arjuna.ats.internal.jta.transaction.arjunacore.TransactionManagerImple;
import com.arjuna.ats.internal.jta.transaction.arjunacore.UserTransactionImple;
import com.atomikos.jdbc.AtomikosDataSourceBean;
import com.vladmihalcea.hpjp.hibernate.forum.dto.PostDTO;
import com.vladmihalcea.hpjp.hibernate.logging.LoggingStatementInspector;
import com.vladmihalcea.hpjp.util.DataSourceProxyType;
import com.vladmihalcea.hpjp.util.logging.InlineQueryLogEntryCreator;
import dev.snowdrop.boot.narayana.core.jdbc.GenericXADataSourceWrapper;
import io.hypersistence.utils.hibernate.type.util.ClassImportIntegrator;
import net.ttddyy.dsproxy.listener.logging.SLF4JQueryLoggingListener;
import net.ttddyy.dsproxy.support.ProxyDataSourceBuilder;
import org.hibernate.cfg.AvailableSettings;
import org.hibernate.engine.transaction.jta.platform.internal.JBossStandAloneJtaPlatform;
import org.hibernate.jpa.HibernatePersistenceProvider;
import org.hibernate.jpa.boot.spi.IntegratorProvider;
import org.postgresql.xa.PGXADataSource;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.*;
import org.springframework.context.support.PropertySourcesPlaceholderConfigurer;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.springframework.orm.jpa.JpaVendorAdapter;
import org.springframework.orm.jpa.LocalContainerEntityManagerFactoryBean;
import org.springframework.orm.jpa.vendor.HibernateJpaVendorAdapter;
import org.springframework.transaction.annotation.EnableTransactionManagement;
import org.springframework.transaction.jta.JtaTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import javax.sql.DataSource;
import java.util.Arrays;
import java.util.Collections;
import java.util.Properties;

/**
 *
 * @author Vlad Mihalcea
 */
@Configuration
@PropertySource({"/META-INF/jta-postgresql.properties"})
@ComponentScan(basePackages = "com.vladmihalcea.hpjp.spring.transaction.jta.narayana")
@EnableTransactionManagement
@EnableAspectJAutoProxy
public class NarayanaJTATransactionManagerConfiguration {

    public static final String DATA_SOURCE_PROXY_NAME = DataSourceProxyType.DATA_SOURCE_PROXY.name();

    @Value("${jdbc.dataSourceClassName}")
    protected String dataSourceClassName;

    @Value("${jdbc.username}")
    protected String jdbcUser;

    @Value("${jdbc.password}")
    protected String jdbcPassword;

    @Value("${jdbc.database}")
    protected String jdbcDatabase;

    @Value("${jdbc.host}")
    protected String jdbcHost;

    @Value("${jdbc.port}")
    protected String jdbcPort;

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

    @Bean
    public DataSource actualDataSource() {
        try {
            PGXADataSource dataSource = new PGXADataSource();
            dataSource.setUrl(
                String.format(
                    "jdbc:postgresql://%s:%s/%s",
                    jdbcHost,
                    jdbcPort,
                    jdbcDatabase
                )
            );
            dataSource.setUser(jdbcUser);
            dataSource.setPassword(jdbcPassword);

            XARecoveryModule xaRecoveryModule = new XARecoveryModule();
            GenericXADataSourceWrapper wrapper = new GenericXADataSourceWrapper(xaRecoveryModule);
            return wrapper.wrapDataSource(dataSource);
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
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

    @Bean
    public UserTransactionImple jtaUserTransaction() {
        UserTransactionImple userTransactionManager = new UserTransactionImple();
        return userTransactionManager;
    }

    @Bean
    public TransactionManagerImple jtaTransactionManager() {
        TransactionManagerImple transactionManager = new TransactionManagerImple();
        return transactionManager;
    }

    @Bean
    public JtaTransactionManager transactionManager() {
        JtaTransactionManager jtaTransactionManager = new JtaTransactionManager();
        jtaTransactionManager.setTransactionManager(jtaTransactionManager());
        jtaTransactionManager.setUserTransaction(jtaUserTransaction());
        jtaTransactionManager.setAllowCustomIsolationLevels(true);
        return jtaTransactionManager;
    }

    @Bean
    public TransactionTemplate transactionTemplate() {
        return new TransactionTemplate(transactionManager());
    }

    protected Properties additionalProperties() {
        Properties properties = new Properties();
        properties.put(
            AvailableSettings.JTA_PLATFORM,
            JBossStandAloneJtaPlatform.class
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
        return properties;
    }

    protected String[] packagesToScan() {
        return new String[]{
            "com.vladmihalcea.hpjp.hibernate.transaction.forum"
        };
    }
}
