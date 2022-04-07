package com.vladmihalcea.book.hpjp.hibernate.connection;

import com.vladmihalcea.flexypool.FlexyPoolDataSource;
import org.junit.After;
import org.junit.Before;
import org.junit.Ignore;
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

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import javax.sql.DataSource;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicLong;

import static com.vladmihalcea.book.hpjp.hibernate.connection.jta.FlexyPoolEntities.Post;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(classes = FlexyPoolTestConfiguration.class)
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
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

        AtomicLong postCount = new AtomicLong();

        for (int i = 0; i < threadCount; i++) {
            tasks.add(
                () -> {
                    LOGGER.info("Starting worker thread");
                    
                    while (TimeUnit.NANOSECONDS.toSeconds(System.nanoTime() - startNanos) < seconds) {
                        transactionTemplate.execute((TransactionCallback<Void>) transactionStatus -> {
                            for (int j = 0; j < 100; j++) {
                                entityManager.persist(new Post());
                            }

                            postCount.set(
                                entityManager.createQuery(
                                    "select count(p) " +
                                    "from Post p ", Number.class)
                                .getSingleResult()
                                .longValue()
                            );

                            if (postCount.get() % 1000 == 0) {
                                LOGGER.info("Post entity count: {}", postCount);
                                sleep(250, TimeUnit.MILLISECONDS);
                            }

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

    private void sleep(long duration, TimeUnit timeUnit) {
        try {
            Thread.sleep(timeUnit.toMillis(duration));
        } catch (InterruptedException e) {
            Thread.interrupted();
        }
    }
}
