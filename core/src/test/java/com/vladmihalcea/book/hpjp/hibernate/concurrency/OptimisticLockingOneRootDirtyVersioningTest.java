package com.vladmihalcea.book.hpjp.hibernate.concurrency;

import com.vladmihalcea.book.hpjp.util.AbstractTest;
import org.hibernate.StaleObjectStateException;
import org.hibernate.annotations.DynamicUpdate;
import org.hibernate.annotations.OptimisticLockType;
import org.hibernate.annotations.OptimisticLocking;
import org.junit.Test;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Table;
import java.math.BigDecimal;
import java.util.concurrent.CountDownLatch;

/**
 * OptimisticLockingOneRootDirtyVersioningTest - Test to check optimistic checking on a single entity being updated by many threads
 * using the dirty properties instead of a synthetic version column
 *
 * @author Vlad Mihalcea
 */
public class OptimisticLockingOneRootDirtyVersioningTest extends AbstractTest {

    private final CountDownLatch loadProductsLatch = new CountDownLatch(3);
    private final CountDownLatch aliceLatch = new CountDownLatch(1);

    public class AliceTransaction implements Runnable {

        @Override
        public void run() {
            try {
                doInJPA(entityManager -> {
                    try {
                        Product product = (Product) entityManager.find(Product.class, 1L);
                        loadProductsLatch.countDown();
                        loadProductsLatch.await();
                        product.setQuantity(6L);
                    } catch (InterruptedException e) {
                        throw new IllegalStateException(e);
                    }
                });
            } catch (StaleObjectStateException expected) {
                LOGGER.info("Alice: Optimistic locking failure", expected);
            }
            aliceLatch.countDown();
        }
    }

    public class BobTransaction implements Runnable {

        @Override
        public void run() {
            try {
                doInJPA(entityManager -> {
                    try {
                        Product product = (Product) entityManager.find(Product.class, 1L);
                        loadProductsLatch.countDown();
                        loadProductsLatch.await();
                        aliceLatch.await();
                        product.incrementLikes();
                    } catch (InterruptedException e) {
                        throw new IllegalStateException(e);
                    }
                });
            } catch (StaleObjectStateException expected) {
                LOGGER.info("Bob: Optimistic locking failure", expected);
            }
        }
    }

    public class CarolTransaction implements Runnable {

        @Override
        public void run() {
            try {
                doInJPA(entityManager -> {
                    try {
                        Product product = (Product) entityManager.find(Product.class, 1L);
                        loadProductsLatch.countDown();
                        loadProductsLatch.await();
                        aliceLatch.await();
                        product.setDescription("Plasma HDTV");
                    } catch (InterruptedException e) {
                        throw new IllegalStateException(e);
                    }
                });
            } catch (StaleObjectStateException expected) {
                LOGGER.info("Carol: Optimistic locking failure", expected);
            }
        }
    }

    @Test
    public void testOptimisticLocking() throws InterruptedException {

        doInJPA(entityManager -> {
            Product product = new Product();
            product.setId(1L);
            product.setName("TV");
            product.setDescription("Plasma TV");
            product.setPrice(BigDecimal.valueOf(199.99));
            product.setQuantity(7L);
            entityManager.persist(product);
        });

        Thread alice = new Thread(new AliceTransaction());
        Thread bob = new Thread(new BobTransaction());
        Thread carol = new Thread(new CarolTransaction());

        alice.start();
        bob.start();
        carol.start();

        alice.join();
        bob.join();
        carol.join();
    }

    @Override
    protected Class<?>[] entities() {
        return new Class<?>[]{
                Product.class
        };
    }

    /**
     * Product - Product
     *
     * @author Vlad Mihalcea
     */
    @Entity(name = "product")
    @Table(name = "product")
    @OptimisticLocking(type = OptimisticLockType.DIRTY)
    @DynamicUpdate
    public static class Product {

        @Id
        private Long id;

        @Column(unique = true, nullable = false)
        private String name;

        @Column(nullable = false)
        private String description;

        @Column(nullable = false)
        private BigDecimal price;

        private long quantity;

        private int likes;

        public Product() {
        }

        public Long getId() {
            return id;
        }

        public void setId(Long id) {
            this.id = id;
        }

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
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

        public long getQuantity() {
            return quantity;
        }

        public void setQuantity(long quantity) {
            this.quantity = quantity;
        }

        public int getLikes() {
            return likes;
        }

        public int incrementLikes() {
            return ++likes;
        }
    }
}
