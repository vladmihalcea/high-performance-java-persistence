package com.vladmihalcea.book.hpjp.hibernate.connection.jta;

import com.codahale.metrics.MetricRegistry;
import com.codahale.metrics.Slf4jReporter;
import com.codahale.metrics.Timer;
import com.vladmihalcea.book.hpjp.util.spring.config.HsqldbJtaTransactionManagerConfiguration;
import com.vladmihalcea.book.hpjp.util.spring.config.PostgreSQLJtaTransactionManagerConfiguration;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.transaction.support.TransactionCallback;
import org.springframework.transaction.support.TransactionTemplate;

import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

import static org.junit.Assert.assertEquals;

@RunWith(SpringJUnit4ClassRunner.class)
//@ContextConfiguration(locations = {"classpath:spring/applicationContext-tx.xml"})
@ContextConfiguration(classes = HsqldbJtaTransactionManagerConfiguration.class)
//@ContextConfiguration(classes = PostgreSQLJtaTransactionManagerConfiguration.class)
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
public class JtaConnectionReleaseTest {

    protected final org.slf4j.Logger LOGGER = LoggerFactory.getLogger(getClass());

    @PersistenceContext
    private EntityManager entityManager;

    @Autowired
    private TransactionTemplate transactionTemplate;

    private int batch = 30;

    private MetricRegistry metricRegistry = new MetricRegistry();

    private Timer timer = metricRegistry.timer("statement");

    private Slf4jReporter logReporter = Slf4jReporter
            .forRegistry(metricRegistry)
            .outputTo(LOGGER)
            .build();

    @Test
    public void test() {
        AtomicLong id = new AtomicLong();
        int seconds = 60;
        long ttl = System.nanoTime() + TimeUnit.SECONDS.toNanos(seconds);
        while (System.nanoTime() < ttl) {
            transactionTemplate.execute((TransactionCallback<Void>) transactionStatus -> {
                for (int i = 0; i < batch; i++) {
                    entityManager.persist(new JtaEntity(id.incrementAndGet()));
                    long startNanos = System.nanoTime();
                    //assertEquals(id.get(), entityManager.createQuery("select count(j) from JtaEntity j", Number.class).getSingleResult().longValue());
                    entityManager.flush();
                    timer.update(System.nanoTime() - startNanos, TimeUnit.NANOSECONDS);
                }
                return null;
            });
        }
        logReporter.report();
        LOGGER.info("Throughput {} statements/second", (id.get()) / seconds);
    }

}
