package com.vladmihalcea.book.hpjp.hibernate.concurrency;

import org.hibernate.*;
import org.hibernate.dialect.lock.OptimisticEntityLockException;
import org.junit.Test;

import java.sql.PreparedStatement;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * LockModeOptimisticWithPessimisticLockUpgradeTest - Test to check LockMode.OPTIMISTIC with pessimistic lock upgrade
 *
 * @author Vlad Mihalcea
 */
public class LockModeOptimisticRaceConditionTest extends AbstractLockModeOptimisticTest {

    private AtomicBoolean ready = new AtomicBoolean();
    private final CountDownLatch endLatch = new CountDownLatch(1);

    @Override
    protected Interceptor interceptor() {
        return new EmptyInterceptor() {
            @Override
            public void beforeTransactionCompletion(Transaction tx) {
                if(ready.get()) {
                    LOGGER.info("Overwrite product price asynchronously");

                    executeAsync(() -> {
                        Session _session = entityManagerFactory().unwrap(SessionFactory.class).openSession();
                        _session.doWork(connection -> {
                            try (PreparedStatement ps = connection.prepareStatement("UPDATE product set price = 14.49 WHERE id = 1")) {
                                ps.executeUpdate();
                            }
                        });
                        _session.close();
                        endLatch.countDown();
                    });
                    try {
                        LOGGER.info("Wait 500 ms for lock to be acquired!");
                        Thread.sleep(500);
                    } catch (InterruptedException e) {
                        throw new IllegalStateException(e);
                    }
                }
            }
        };
    }

    @Test
    public void testExplicitOptimisticLocking() throws InterruptedException {
        try {
            doInJPA(entityManager -> {
                try {
                    Session session = entityManager.unwrap(Session.class);
                    final Product product = (Product) session.get(Product.class, 1L, new LockOptions(LockMode.OPTIMISTIC));
                    OrderLine orderLine = new OrderLine(product);
                    entityManager.persist(orderLine);
                    lockUpgrade(session, product);
                    ready.set(true);
                } catch (Exception e) {
                    throw new IllegalStateException(e);
                }
            });
        } catch (OptimisticEntityLockException expected) {
            LOGGER.info("Failure: ", expected);
        }
        endLatch.await();
    }

    protected void lockUpgrade(Session session, Product product) {}
}
