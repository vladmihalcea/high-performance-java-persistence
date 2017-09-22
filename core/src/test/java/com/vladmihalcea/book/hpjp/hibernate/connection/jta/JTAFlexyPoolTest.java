package com.vladmihalcea.book.hpjp.hibernate.connection.jta;

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
@ContextConfiguration(classes = JTAFlexyPoolTestConfiguration.class)
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
public class JTAFlexyPoolTest {

    protected final Logger LOGGER = LoggerFactory.getLogger(getClass());

    @PersistenceContext
    private EntityManager entityManager;

    @Autowired
    private TransactionTemplate transactionTemplate;

    @Autowired
    private DataSource dataSource;

    private int threadCount = 10;
    private int seconds = 60;

    private ExecutorService executorService = Executors.newFixedThreadPool(threadCount);

    @Before
    public void init() {
        FlexyPoolDataSource flexyPoolDataSource = (FlexyPoolDataSource) dataSource;
        flexyPoolDataSource.start();
    }

    @After
    public void destroy() {
        FlexyPoolDataSource flexyPoolDataSource = (FlexyPoolDataSource) dataSource;
        flexyPoolDataSource.stop();
    }

    @Test
    public void test() throws InterruptedException, ExecutionException {
        long startNanos = System.nanoTime();
       while (TimeUnit.NANOSECONDS.toSeconds(System.nanoTime() - startNanos) < seconds){
            List<Future<Void>> futures = new ArrayList<>();
            for (int i = 0; i < threadCount; i++) {
                futures.add(executorService.submit((Callable<Void>) () -> {
                    transactionTemplate.execute((TransactionCallback<Void>) transactionStatus -> {
                        for (int j = 0; j < 1000; j++) {
                            entityManager.persist(new FlexyPoolEntities.Post());
                        }
                        entityManager.createQuery("select count(p) from Post p").getSingleResult();
                        return null;
                    });
                    return null;
                }));
            }
            for (Future<Void> future : futures) {
                future.get();
            }
        }
    }
}
