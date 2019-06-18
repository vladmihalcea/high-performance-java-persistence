package com.vladmihalcea.book.hpjp.hibernate.concurrency.version;

import com.vladmihalcea.book.hpjp.util.AbstractTest;
import org.hibernate.annotations.Type;
import org.hibernate.annotations.TypeDef;
import org.junit.Test;

import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Table;
import javax.persistence.Version;

import static org.junit.Assert.assertEquals;

/**
 * @author Vlad Mihalcea
 */
public class MinValueIntegerVersionTest extends AbstractTest {

    @Override
    protected Class<?>[] entities() {
        return new Class<?>[]{
                Product.class
        };
    }

    @Test
    public void testOptimisticLocking() {

        doInJPA(entityManager -> {
            entityManager.persist(
                new Product()
                    .setId(1L)
                    .setQuantity(10)
                    .setVersion(Integer.MAX_VALUE)
            );
        });

        doInJPA(entityManager -> {
            Product product = entityManager.find(Product.class, 1L);

            assertEquals(Integer.MAX_VALUE, product.getVersion());

            product.setQuantity(9);
        });

        doInJPA(entityManager -> {
            Product product = entityManager.find(Product.class, 1L);

            assertEquals(Integer.MIN_VALUE, product.getVersion());
        });
    }

    @Entity(name = "Product")
    @Table(name = "product")
    public static class Product {

        @Id
        private Long id;

        private int quantity;

        @Version
        private int version;

        public Long getId() {
            return id;
        }

        public Product setId(Long id) {
            this.id = id;
            return this;
        }

        public int getQuantity() {
            return quantity;
        }

        public Product setQuantity(int quantity) {
            this.quantity = quantity;
            return this;
        }

        public int getVersion() {
            return version;
        }

        public Product setVersion(int version) {
            this.version = version;
            return this;
        }
    }
}
