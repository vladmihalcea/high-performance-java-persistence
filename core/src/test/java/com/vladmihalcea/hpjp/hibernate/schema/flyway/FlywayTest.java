package com.vladmihalcea.hpjp.hibernate.schema.flyway;

import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import jakarta.persistence.PersistenceContext;
import org.hibernate.boot.spi.MetadataImplementor;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.jpa.boot.spi.Bootstrap;
import org.hibernate.jpa.boot.spi.EntityManagerFactoryBuilder;
import org.hibernate.service.spi.ServiceRegistryImplementor;
import org.hibernate.tool.schema.internal.ExceptionHandlerHaltImpl;
import org.hibernate.tool.schema.spi.*;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.orm.jpa.LocalContainerEntityManagerFactoryBean;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.transaction.TransactionException;
import org.springframework.transaction.support.TransactionCallback;
import org.springframework.transaction.support.TransactionTemplate;

import java.util.Collections;
import java.util.Map;

import static com.vladmihalcea.hpjp.hibernate.schema.flyway.FlywayEntities.Post;

/**
 * @author Vlad Mihalcea
 */
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(classes = PostgreSQLFlywayConfiguration.class)
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
public class FlywayTest {

    protected final Logger LOGGER = LoggerFactory.getLogger(getClass());

    @PersistenceContext
    private EntityManager entityManager;

    @Autowired
    private TransactionTemplate transactionTemplate;

    @Autowired
    private LocalContainerEntityManagerFactoryBean localContainerEntityManagerFactoryBean;

    @Test
    public void test() {
        try {
            transactionTemplate.execute((TransactionCallback<Void>) transactionStatus -> {
                Post post = new Post();
                entityManager.persist(post);
                return null;
            });
        } catch (TransactionException e) {
            LOGGER.error("Failure", e);
        }
    }

    @Test
    @Ignore
    public void testValidate() {
        Map<String, Object> settings = localContainerEntityManagerFactoryBean.getJpaPropertyMap();
        EntityManagerFactoryBuilder entityManagerFactoryBuilder = Bootstrap.getEntityManagerFactoryBuilder(
            localContainerEntityManagerFactoryBean.getPersistenceUnitInfo(),
            settings
        );
        MetadataImplementor metadataImplementor = entityManagerFactoryBuilder.metadata();
        EntityManagerFactory entityManagerFactory = entityManagerFactoryBuilder.build();

        SessionFactoryImplementor sessionFactory = entityManagerFactory.unwrap(SessionFactoryImplementor.class);
        ServiceRegistryImplementor serviceRegistry = sessionFactory.getServiceRegistry();
        SchemaManagementTool tool = serviceRegistry.getService(SchemaManagementTool.class);
        Map<String,Object> options = Collections.emptyMap();
        SchemaValidator schemaValidator = tool.getSchemaValidator(options);

        final ExecutionOptions executionOptions = SchemaManagementToolCoordinator.buildExecutionOptions(
            settings,
            ExceptionHandlerHaltImpl.INSTANCE
        );

        schemaValidator.doValidation(
            metadataImplementor,
            executionOptions,
            contributed -> contributed.getContributor().equals("orm")
        );
    }
}
