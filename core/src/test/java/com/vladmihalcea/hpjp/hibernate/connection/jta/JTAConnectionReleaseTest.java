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

import java.util.concurrent.TimeUnit;

import static org.junit.Assert.assertNotNull;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(classes = AtomikosJTAConnectionReleaseConfiguration.class)
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
public class JTAConnectionReleaseTest {

    protected final Logger LOGGER = LoggerFactory.getLogger(getClass());

    @PersistenceContext
    private EntityManager entityManager;

    @Autowired
    private TransactionTemplate transactionTemplate;

    private int[] batches = {10, 50, 100, 500, 1000, 5000};

    @Test
    public void test() {
        //Warming up
        for (int i = 0; i < 1000; i++) {
            transactionTemplate.execute((TransactionCallback<Void>) transactionStatus -> {
                assertNotNull(entityManager.createNativeQuery("select now()").getSingleResult());
                return null;
            });
        }
        for (int batch : batches) {
            long startNanos = System.nanoTime();
            transactionTemplate.execute((TransactionCallback<Void>) transactionStatus -> {
                for (int i = 0; i < batch; i++) {
                    assertNotNull(entityManager.createNativeQuery("select now()").getSingleResult());
                }
                return null;
            });
            LOGGER.info(
                "Transaction took {} millis for {} statements",
                TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - startNanos),
                batch
            );
        }
        transactionTemplate.execute((TransactionCallback<Void>) transactionStatus -> {
            entityManager.unwrap(Session.class).getSessionFactory().getStatistics().logSummary();
            return null;
        });
    }
}
