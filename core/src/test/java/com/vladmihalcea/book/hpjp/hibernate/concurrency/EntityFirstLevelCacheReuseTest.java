package com.vladmihalcea.book.hpjp.hibernate.concurrency;

import com.vladmihalcea.book.hpjp.util.AbstractTest;
import org.junit.Test;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import static org.junit.Assert.*;

/**
 * OptimisticLockingTest - Test to check optimistic checking
 *
 * @author Vlad Mihalcea
 */
public class EntityFirstLevelCacheReuseTest extends AbstractTest {

    @Override
    protected Class<?>[] entities() {
        return new Class<?>[] {
            Product.class
        };
    }

    @Test
    public void testOptimisticLocking() {
        doInJPA(entityManager -> {
            Product product = new Product();
            product.setId(1L);
            product.setQuantity(7L);
            entityManager.persist(product);
        });
        doInJPA(entityManager -> {
            final Product product = (Product) entityManager.find(Product.class, 1L);
            try {
                executeSync( () -> doInJPA(_entityManager -> {
                    Product otherThreadProduct = (Product) _entityManager.find(Product.class, 1L);
                    assertNotSame(product, otherThreadProduct);
                    otherThreadProduct.setQuantity(6L);
                }));

                Product reloadedProduct = (Product) entityManager.createQuery("select p from Product p").getSingleResult();
                assertEquals(7L, reloadedProduct.getQuantity());
                assertEquals(6L,
                        ((Number) entityManager
                                .createNativeQuery("select quantity from product where id = :id")
                                .setParameter("id", product.getId())
                                .getSingleResult())
                                .longValue()
                );
            } catch (Exception e) {
                fail(e.getMessage());
            }
        });
    }

    @Entity(name = "Product")
    @Table(name = "product")
    public static class Product {

        @Id
        private Long id;

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
