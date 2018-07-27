package com.vladmihalcea.book.hpjp.hibernate.connection.jta;

import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.TransactionCallback;
import org.springframework.transaction.support.TransactionTemplate;

import javax.persistence.EntityManagerFactory;
import javax.persistence.PersistenceUnit;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(classes = JTAConnectionReleaseConfiguration.class)
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
public class JtaMultipleTransactionsTest {

    protected final Logger LOGGER = LoggerFactory.getLogger(getClass());

    @PersistenceUnit
    private EntityManagerFactory entityManagerFactory;

    @Autowired
    private TransactionTemplate transactionTemplate;

    @Test
    @Ignore
    public void testManualTxManagement() {

        try(Session session1 = entityManagerFactory.unwrap(SessionFactory.class).openSession()) {
            session1.beginTransaction();

            try(Session session2 = entityManagerFactory.unwrap(SessionFactory.class).openSession()) {
                session2.beginTransaction();
                session2.getTransaction().commit();

            }
            session1.getTransaction().commit();
        }
    }

    @Test
    @Ignore
    public void testAutoTxManagement() {

        transactionTemplate.execute((TransactionCallback<Void>) transactionStatus1 -> {
            try(Session session1 = entityManagerFactory.unwrap(SessionFactory.class).openSession()) {
                transactionTemplate.execute((TransactionCallback<Void>) transactionStatus2 -> {
                    try(Session session2 = entityManagerFactory.unwrap(SessionFactory.class).openSession()) {
                    }
                    return null;
                });
            }
            return null;
        });

    }
}
