package com.vladmihalcea.book.hpjp.hibernate.concurrency;

import com.vladmihalcea.book.hpjp.util.AbstractPostgreSQLIntegrationTest;
import org.hibernate.LockMode;
import org.hibernate.LockOptions;
import org.hibernate.Session;
import org.hibernate.StaleObjectStateException;
import org.junit.Before;
import org.junit.Test;

import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Table;
import javax.persistence.Version;
import java.math.BigDecimal;
import java.util.concurrent.CountDownLatch;


/**
 * LockModePessimisticReadWriteIntegrationTest - Test to check LockMode.PESSIMISTIC_READ and LockMode.PESSIMISTIC_WRITE
 *
 * @author Carol Mihalcea
 */
public class LockModePessimisticReadWriteIntegrationTest extends AbstractPostgreSQLIntegrationTest {

    public static final int WAIT_MILLIS = 500;

    private static interface ProductLockRequestCallable {
        void lock(Session session, Product product);
    }

    private final CountDownLatch endLatch = new CountDownLatch(1);

    @Override
    protected Class<?>[] entities() {
        return new Class<?>[]{
                Product.class
        };
    }

    @Before
    public void init() {
        super.init();
        doInJPA(entityManager -> {
            Product product = new Product();
            product.setId(1L);
            product.setDescription("USB Flash Drive");
            product.setPrice(BigDecimal.valueOf(12.99));
            entityManager.persist(product);
        });
    }

    private void testPessimisticLocking(ProductLockRequestCallable primaryLockRequestCallable, ProductLockRequestCallable secondaryLockRequestCallable) {
        doInJPA(entityManager -> {
            try {
                Session session = entityManager.unwrap(Session.class);
                Product product = (Product) entityManager.find(Product.class, 1L);
                primaryLockRequestCallable.lock(session, product);
                executeAsync(
                        () -> {
                            doInJPA(_entityManager -> {
                                Session _session = _entityManager.unwrap(Session.class);
                                Product _product = (Product) _entityManager.find(Product.class, 1L);
                                secondaryLockRequestCallable.lock(_session, _product);
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
    public void testPessimisticReadDoesNotBlockPessimisticRead() throws InterruptedException {
        LOGGER.info("Test PESSIMISTIC_READ doesn't block PESSIMISTIC_READ");
        testPessimisticLocking(
                (session, product) -> {
                    session.buildLockRequest(new LockOptions(LockMode.PESSIMISTIC_READ)).lock(product);
                    LOGGER.info("PESSIMISTIC_READ acquired");
                },
                (session, product) -> {
                    session.buildLockRequest(new LockOptions(LockMode.PESSIMISTIC_READ)).lock(product);
                    LOGGER.info("PESSIMISTIC_READ acquired");
                }
        );
    }

    @Test
    public void testPessimisticReadBlocksUpdate() throws InterruptedException {
        LOGGER.info("Test PESSIMISTIC_READ blocks UPDATE");
        testPessimisticLocking(
                (session, product) -> {
                    session.buildLockRequest(new LockOptions(LockMode.PESSIMISTIC_READ)).lock(product);
                    LOGGER.info("PESSIMISTIC_READ acquired");
                },
                (session, product) -> {
                    product.setDescription("USB Flash Memory Stick");
                    session.flush();
                    LOGGER.info("Implicit lock acquired");
                }
        );
    }

    @Test
    public void testPessimisticReadWithPessimisticWriteNoWait() throws InterruptedException {
        LOGGER.info("Test PESSIMISTIC_READ blocks PESSIMISTIC_WRITE, NO WAIT fails fast");
        testPessimisticLocking(
                (session, product) -> {
                    session.buildLockRequest(new LockOptions(LockMode.PESSIMISTIC_READ)).lock(product);
                    LOGGER.info("PESSIMISTIC_READ acquired");
                },
                (session, product) -> {
                    session.buildLockRequest(new LockOptions(LockMode.PESSIMISTIC_WRITE)).setTimeOut(Session.LockRequest.PESSIMISTIC_NO_WAIT).lock(product);
                    LOGGER.info("PESSIMISTIC_WRITE acquired");
                }
        );
    }

    @Test
    public void testPessimisticWriteBlocksPessimisticRead() throws InterruptedException {
        LOGGER.info("Test PESSIMISTIC_WRITE blocks PESSIMISTIC_READ");
        testPessimisticLocking(
                (session, product) -> {
                    session.buildLockRequest(new LockOptions(LockMode.PESSIMISTIC_WRITE)).lock(product);
                    LOGGER.info("PESSIMISTIC_WRITE acquired");
                },
                (session, product) -> {
                    session.buildLockRequest(new LockOptions(LockMode.PESSIMISTIC_READ)).lock(product);
                    LOGGER.info("PESSIMISTIC_READ acquired");
                }
        );
    }

    @Test
    public void testPessimisticWriteBlocksPessimisticWrite() throws InterruptedException {
        LOGGER.info("Test PESSIMISTIC_WRITE blocks PESSIMISTIC_WRITE");
        testPessimisticLocking(
                (session, product) -> {
                    session.buildLockRequest(new LockOptions(LockMode.PESSIMISTIC_WRITE)).lock(product);
                    LOGGER.info("PESSIMISTIC_WRITE acquired");
                },
                (session, product) -> {
                    session.buildLockRequest(new LockOptions(LockMode.PESSIMISTIC_WRITE)).lock(product);
                    LOGGER.info("PESSIMISTIC_WRITE acquired");
                }
        );
    }

    /**
     * Product - Product
     *
     * @author Carol Mihalcea
     */
    @Entity(name = "Product")
    @Table(name = "product")
    public static class Product {

        @Id
        private Long id;

        private String description;

        private BigDecimal price;

        @Version
        private int version;

        public Long getId() {
            return id;
        }

        public void setId(Long id) {
            this.id = id;
        }

        public String getDescription() {
            return description;
        }

        public void setDescription(String description) {
            this.description = description;
        }

        public BigDecimal getPrice() {
            return price;
        }

        public void setPrice(BigDecimal price) {
            this.price = price;
        }
    }
}
