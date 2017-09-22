package com.vladmihalcea.book.hpjp.hibernate.connection;

import com.vladmihalcea.book.hpjp.hibernate.connection.jta.FlexyPoolEntities;
import com.vladmihalcea.flexypool.FlexyPoolDataSource;

import org.junit.After;
import org.junit.Before;
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

import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import javax.sql.DataSource;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.*;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(classes = FlexyPoolTestConfiguration.class)
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
public class FlexyPoolTest {

    protected final Logger LOGGER = LoggerFactory.getLogger(getClass());

    @PersistenceContext
    private EntityManager entityManager;

    @Autowired
    private TransactionTemplate transactionTemplate;

    @Autowired
    private DataSource dataSource;

    private int threadCount = 6;

    private int seconds = 120;

    private ExecutorService executorService = Executors.newFixedThreadPool(threadCount);

    @Before
    public void init() {
        FlexyPoolDataSource flexyPoolDataSource = (FlexyPoolDataSource) dataSource;
        flexyPoolDataSource.start();
    }

    @After
    public void destroy() {
        executorService.shutdownNow();
        FlexyPoolDataSource flexyPoolDataSource = (FlexyPoolDataSource) dataSource;
        flexyPoolDataSource.stop();
    }

    @Test
    public void test() throws InterruptedException, ExecutionException {
        long startNanos = System.nanoTime();

        CountDownLatch awaitTermination = new CountDownLatch(threadCount);
        List<Callable<Void>> tasks = new ArrayList<>();

        for (int i = 0; i < threadCount; i++) {
            tasks.add(
                () -> {
                    LOGGER.info("Starting worker thread");
                    while (TimeUnit.NANOSECONDS.toSeconds(System.nanoTime() - startNanos) < seconds) {
                        transactionTemplate.execute((TransactionCallback<Void>) transactionStatus -> {
                            for (int j = 0; j < 50; j++) {
                                entityManager.persist(new FlexyPoolEntities.Post());
                            }
                            Number postCount = entityManager.createQuery(
                                "select count(p) from Post p", Number.class)
                            .getSingleResult();
                            LOGGER.info("Post entity count: {}", postCount);
                            return null;
                        });
                    }
                    awaitTermination.countDown();
                    return null;
                }
            );
        }

        executorService.invokeAll(tasks);
        awaitTermination.await();
    }
}
