package com.vladmihalcea.hpjp.hibernate.connection.jta;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.hibernate.Session;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.transaction.support.TransactionCallback;
import org.springframework.transaction.support.TransactionTemplate;

import javax.sql.DataSource;
import java.sql.Connection;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(classes = AtomikosJTAConnectionReleaseConfiguration.class)
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
public class JTAConnectionThreadBoundTest {

    protected final Logger LOGGER = LoggerFactory.getLogger(getClass());

    @PersistenceContext
    private EntityManager entityManager;

    @Autowired
    private TransactionTemplate transactionTemplate;

    @Autowired
    private DataSource dataSource;

    @Test
    public void test() {
        transactionTemplate.execute((TransactionCallback<Void>) transactionStatus -> {
            entityManager.unwrap(Session.class).doWork(connection -> {
                try(Connection anotherConnection = dataSource.getConnection()) {
                    LOGGER.info("Connections got from JTA transactions are{} bound to thread", connection == anotherConnection ? "" : " not");
                }
            });

            return null;
        });
    }
}
