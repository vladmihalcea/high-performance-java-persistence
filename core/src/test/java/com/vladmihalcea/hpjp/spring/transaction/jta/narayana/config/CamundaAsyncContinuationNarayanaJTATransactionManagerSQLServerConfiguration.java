package com.vladmihalcea.hpjp.spring.transaction.jta.narayana.config;

import com.arjuna.ats.internal.jta.recovery.arjunacore.XARecoveryModule;
import com.vladmihalcea.hpjp.util.logging.InlineQueryLogEntryCreator;
import dev.snowdrop.boot.narayana.core.jdbc.GenericXADataSourceWrapper;
import jakarta.transaction.SystemException;
import net.ttddyy.dsproxy.listener.logging.SLF4JQueryLoggingListener;
import net.ttddyy.dsproxy.support.ProxyDataSourceBuilder;
import org.camunda.bpm.engine.*;
import org.camunda.bpm.engine.spring.ProcessEngineFactoryBean;
import org.camunda.bpm.engine.spring.SpringProcessEngineConfiguration;
import org.postgresql.xa.PGXADataSource;
import org.springframework.context.annotation.*;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.transaction.annotation.EnableTransactionManagement;
import org.springframework.web.client.RestTemplate;

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
public class CamundaAsyncContinuationNarayanaJTATransactionManagerSQLServerConfiguration extends CamundaNarayanaJTATransactionManagerSQLServerConfiguration {

    @Bean
    public RestTemplate restTemplate() {
        return new RestTemplate();
    }

    protected ClassPathResource bpmnResource() {
        return new ClassPathResource("bpmn/forum-post-async-process.bpmn");
    }

}
