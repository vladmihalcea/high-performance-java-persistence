package com.vladmihalcea.book.hpjp.hibernate.schema.flyway;

import com.vladmihalcea.book.hpjp.util.spring.config.jpa.PostgreSQLJPAConfiguration;
import org.hibernate.Session;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.support.EncodedResource;
import org.springframework.jdbc.datasource.init.ScriptUtils;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.transaction.TransactionException;
import org.springframework.transaction.support.TransactionCallback;
import org.springframework.transaction.support.TransactionTemplate;

import javax.annotation.Resource;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;

/**
 * @author Vlad Mihalcea
 */
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(classes = PostgreSQLJPAConfiguration.class)
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
public class DropPostgreSQLPublicSchemaTest {

    protected final Logger LOGGER = LoggerFactory.getLogger(getClass());

    @PersistenceContext
    private EntityManager entityManager;

    @Autowired
    private TransactionTemplate transactionTemplate;

    @Resource
    private String databaseType;

    private boolean drop = true;

    @Test
    public void test() {
        if (drop) {
            try {
                transactionTemplate.execute((TransactionCallback<Void>) transactionStatus -> {
                    Session session = entityManager.unwrap(Session.class);
                    session.doWork(connection -> {
                        ScriptUtils.executeSqlScript(connection,
                            new EncodedResource(
                                new ClassPathResource(
                                    String.format("flyway/scripts/%1$s/drop/drop.sql", databaseType)
                                )
                            ),
                            true, true,
                            ScriptUtils.DEFAULT_COMMENT_PREFIX,
                            ScriptUtils.DEFAULT_BLOCK_COMMENT_START_DELIMITER,
                            ScriptUtils.DEFAULT_BLOCK_COMMENT_END_DELIMITER,
                            ScriptUtils.DEFAULT_COMMENT_PREFIX);
                    });
                    return null;
                });
            } catch (TransactionException e) {
                LOGGER.error("Failure", e);
            }
        }
    }
}
