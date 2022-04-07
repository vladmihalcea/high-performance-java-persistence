package com.vladmihalcea.book.hpjp.hibernate.concurrency;

import org.hibernate.*;
import org.hibernate.dialect.lock.OptimisticEntityLockException;
import org.junit.Test;

import jakarta.persistence.EntityManager;
import jakarta.persistence.LockModeType;
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
                        doInJPA(entityManager -> {
                            entityManager.createNativeQuery(
                                "UPDATE post set title = 'High-performance JDBC' WHERE id = :id")
                            .setParameter("id", 1L)
                            .executeUpdate();
                        });
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
                    final Post post = entityManager.find(Post.class, 1L, LockModeType.OPTIMISTIC);
                    PostComment comment = new PostComment();
                    comment.setId(1L);
                    comment.setReview("Good one.");
                    comment.setPost(post);
                    lockUpgrade(entityManager, post);
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

    protected void lockUpgrade(EntityManager entityManager, Post post) {}
}
