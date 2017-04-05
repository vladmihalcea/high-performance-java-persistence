package com.vladmihalcea.book.hpjp.hibernate.concurrency;

import com.vladmihalcea.book.hpjp.util.AbstractOracleXEIntegrationTest;
import com.vladmihalcea.book.hpjp.util.AbstractPostgreSQLIntegrationTest;
import org.hibernate.LockMode;
import org.hibernate.LockOptions;
import org.hibernate.Session;
import org.hibernate.StaleObjectStateException;
import org.hibernate.dialect.lock.PessimisticEntityLockException;

import org.junit.Before;
import org.junit.Test;

import javax.persistence.*;
import java.util.Collections;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;


/**
 * LockModePessimisticReadWriteIntegrationTest - Test to check LockMode.PESSIMISTIC_READ and LockMode.PESSIMISTIC_WRITE
 *
 * @author Vlad Mihalcea
 */
public class LockModePessimisticReadWriteIntegrationTest extends AbstractPostgreSQLIntegrationTest {

    public static final int WAIT_MILLIS = 500;

    private interface LockRequestCallable {
        void lock(Session session, Post post);
    }

    private final CountDownLatch endLatch = new CountDownLatch(1);

    @Override
    protected Class<?>[] entities() {
        return new Class<?>[]{
                Post.class
        };
    }

    @Before
    public void init() {
        super.init();
        doInJPA(entityManager -> {
            Post post = new Post();
            post.setId(1L);
            post.setTitle("High-Performance Java Persistence");
            post.setBody("Chapter 17 summary");
            entityManager.persist(post);
        });
    }

    private void testPessimisticLocking(LockRequestCallable primaryLockRequestCallable, LockRequestCallable secondaryLockRequestCallable) {
        doInJPA(entityManager -> {
            try {
                Session session = entityManager.unwrap(Session.class);
                Post post = entityManager.find(Post.class, 1L);
                primaryLockRequestCallable.lock(session, post);
                executeAsync(
                        () -> {
                            doInJPA(_entityManager -> {
                                Session _session = _entityManager.unwrap(Session.class);
                                Post _post = _entityManager.find(Post.class, 1L);
                                secondaryLockRequestCallable.lock(_session, _post);
                            });
                        },
                        endLatch::countDown
                );
                sleep(WAIT_MILLIS);
            } catch (StaleObjectStateException e) {
                LOGGER.info("Optimistic locking failure: ", e);
            }
        });
        awaitOnLatch(endLatch);
    }

    @Test
    public void testPessimisticRead() {
        LOGGER.info("Test PESSIMISTIC_READ");
        doInJPA(entityManager -> {
            Post post = entityManager.find(Post.class, 1L, LockModeType.PESSIMISTIC_READ);
        });
    }

    @Test
    public void testPessimisticWrite() {
        LOGGER.info("Test PESSIMISTIC_WRITE");
        doInJPA(entityManager -> {
            Post post = entityManager.find(Post.class, 1L, LockModeType.PESSIMISTIC_WRITE);
        });
    }

    @Test
    public void testPessimisticWriteAfterFetch() {
        doInJPA(entityManager -> {
            Post post = entityManager.find(Post.class, 1L);
            entityManager.lock(post, LockModeType.PESSIMISTIC_WRITE);
        });
    }

    @Test
    public void testPessimisticWriteAfterFetchWithDetachedForJPA() {
        Post post = doInJPA(entityManager -> {
            return entityManager.find(Post.class, 1L);
        });
        try {
            doInJPA(entityManager -> {
                entityManager.lock(post, LockModeType.PESSIMISTIC_WRITE);
            });
        } catch (IllegalArgumentException e) {
            assertEquals("entity not in the persistence context", e.getMessage());
        }
    }

    @Test
    public void testPessimisticWriteAfterFetchWithDetachedForHibernate() {
        Post post = doInJPA(entityManager -> {
            return entityManager.find(Post.class, 1L);
        });
        doInJPA(entityManager -> {
            LOGGER.info("Lock and reattach");
            entityManager.unwrap(Session.class)
            .buildLockRequest(
                new LockOptions(LockMode.PESSIMISTIC_WRITE))
            .lock(post);
            post.setTitle("High-Performance Hibernate");
        });
    }

    @Test
    public void testPessimisticReadDoesNotBlockPessimisticRead() throws InterruptedException {
        LOGGER.info("Test PESSIMISTIC_READ doesn't block PESSIMISTIC_READ");
        testPessimisticLocking(
                (session, post) -> {
                    session.buildLockRequest(new LockOptions(LockMode.PESSIMISTIC_READ)).lock(post);
                    LOGGER.info("PESSIMISTIC_READ acquired");
                },
                (session, post) -> {
                    session.buildLockRequest(new LockOptions(LockMode.PESSIMISTIC_READ)).lock(post);
                    LOGGER.info("PESSIMISTIC_READ acquired");
                }
        );
    }

    @Test
    public void testPessimisticReadBlocksUpdate() throws InterruptedException {
        LOGGER.info("Test PESSIMISTIC_READ blocks UPDATE");
        testPessimisticLocking(
                (session, post) -> {
                    session.buildLockRequest(new LockOptions(LockMode.PESSIMISTIC_READ)).lock(post);
                    LOGGER.info("PESSIMISTIC_READ acquired");
                },
                (session, post) -> {
                    post.setBody("Chapter 16 summary");
                    session.flush();
                    LOGGER.info("Implicit lock acquired");
                }
        );
    }

    @Test
    public void testPessimisticReadWithPessimisticWriteNoWait() throws InterruptedException {
        LOGGER.info("Test PESSIMISTIC_READ blocks PESSIMISTIC_WRITE, NO WAIT fails fast");
        testPessimisticLocking(
                (session, post) -> {
                    session.buildLockRequest(new LockOptions(LockMode.PESSIMISTIC_READ)).lock(post);
                    LOGGER.info("PESSIMISTIC_READ acquired");
                },
                (session, post) -> {
                    session.buildLockRequest(new LockOptions(LockMode.PESSIMISTIC_WRITE)).setTimeOut(Session.LockRequest.PESSIMISTIC_NO_WAIT).lock(post);
                    LOGGER.info("PESSIMISTIC_WRITE acquired");
                }
        );
    }

    @Test
    public void testPessimisticWriteBlocksPessimisticRead() throws InterruptedException {
        LOGGER.info("Test PESSIMISTIC_WRITE blocks PESSIMISTIC_READ");
        testPessimisticLocking(
                (session, post) -> {
                    session.buildLockRequest(new LockOptions(LockMode.PESSIMISTIC_WRITE)).lock(post);
                    LOGGER.info("PESSIMISTIC_WRITE acquired");
                },
                (session, post) -> {
                    session.buildLockRequest(new LockOptions(LockMode.PESSIMISTIC_READ)).lock(post);
                    LOGGER.info("PESSIMISTIC_READ acquired");
                }
        );
    }

    @Test
    public void testPessimisticWriteBlocksPessimisticWrite() throws InterruptedException {
        LOGGER.info("Test PESSIMISTIC_WRITE blocks PESSIMISTIC_WRITE");
        testPessimisticLocking(
                (session, post) -> {
                    session.buildLockRequest(new LockOptions(LockMode.PESSIMISTIC_WRITE)).lock(post);
                    LOGGER.info("PESSIMISTIC_WRITE acquired");
                },
                (session, post) -> {
                    session.buildLockRequest(new LockOptions(LockMode.PESSIMISTIC_WRITE)).lock(post);
                    LOGGER.info("PESSIMISTIC_WRITE acquired");
                }
        );
    }

    @Test
    public void testPessimisticNoWait() {
        LOGGER.info("Test PESSIMISTIC_READ blocks PESSIMISTIC_WRITE, NO WAIT fails fast");
        Post post = doInJPA(entityManager -> {
            return entityManager.find(Post.class, 1L);
        });

        doInJPA(entityManager -> {
            entityManager.unwrap( Session.class ).lock(post, LockMode.PESSIMISTIC_WRITE);

            executeSync( () -> {
                doInJPA(_entityManager -> {
                    try {
                        _entityManager
                        .unwrap(Session.class)
                        .buildLockRequest(
                            new LockOptions(LockMode.PESSIMISTIC_WRITE)
                            .setTimeOut(LockOptions.NO_WAIT))
                        .lock(post);
                        fail("Should throw PessimisticEntityLockException");
                    }
                    catch (PessimisticEntityLockException expected) {
                        //This is expected since the first transaction already acquired this lock
                    }
                });
            } );
        });
    }

    @Test
    public void testPessimisticNoWaitJPA() throws InterruptedException {
        LOGGER.info("Test PESSIMISTIC_READ blocks PESSIMISTIC_WRITE, NO WAIT fails fast");
        doInJPA(entityManager -> {
            Post post = entityManager.find(Post.class, 1L);
            entityManager.lock(post, LockModeType.PESSIMISTIC_WRITE,
                Collections.singletonMap("javax.persistence.lock.timeout", 0)
            );
        });
    }

    @Test
    public void testPessimisticTimeout() throws InterruptedException {
        doInJPA(entityManager -> {
            Post post = entityManager.getReference(Post.class, 1L);

            entityManager.unwrap(Session.class)
            .buildLockRequest(
                new LockOptions(LockMode.PESSIMISTIC_WRITE)
                .setTimeOut((int) TimeUnit.SECONDS.toMillis(3)))
            .lock(post);
        });
    }

    @Test
    public void testPessimisticTimeoutJPA() throws InterruptedException {
        doInJPA(entityManager -> {
            Post post = entityManager.find(Post.class, 1L);
            entityManager.lock(post, LockModeType.PESSIMISTIC_WRITE,
                Collections.singletonMap("javax.persistence.lock.timeout",
                    TimeUnit.SECONDS.toMillis(3))
            );
        });
    }

    @Entity(name = "Post")
    @Table(name = "post")
    public static class Post {

        @Id
        private Long id;

        private String title;

        private String body;

        @Version
        private int version;

        public Long getId() {
            return id;
        }

        public void setId(Long id) {
            this.id = id;
        }

        public String getTitle() {
            return title;
        }

        public void setTitle(String title) {
            this.title = title;
        }

        public String getBody() {
            return body;
        }

        public void setBody(String body) {
            this.body = body;
        }
    }
}
