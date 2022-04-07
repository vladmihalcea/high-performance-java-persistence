package com.vladmihalcea.book.hpjp.hibernate.concurrency.version;

import com.vladmihalcea.book.hpjp.util.AbstractTest;
import com.vladmihalcea.book.hpjp.util.providers.Database;
import org.junit.Test;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;

import static org.junit.Assert.assertEquals;

/**
 * @author Vlad Mihalcea
 */
public class DefaultMinValueShortVersionTest extends AbstractTest {

    @Override
    protected Class<?>[] entities() {
        return new Class<?>[]{
                Product.class
        };
    }

    @Override
    protected Database database() {
        return Database.MYSQL;
    }

    @Test
    public void testOptimisticLocking() {

        doInJPA(entityManager -> {
            entityManager.persist(
                new Product()
                    .setId(1L)
                    .setQuantity(10)
                    .setVersion(Short.MAX_VALUE)
            );
        });

        doInJPA(entityManager -> {
            Product product = entityManager.find(Product.class, 1L);

            assertEquals(Short.MAX_VALUE, product.getVersion());

            product.setQuantity(9);
        });

        doInJPA(entityManager -> {
            Product product = entityManager.find(Product.class, 1L);

            assertEquals(Short.MIN_VALUE, product.getVersion());
        });
    }

    @Entity(name = "Product")
    @Table(name = "product")
    public static class Product {

        @Id
        private Long id;

        private int quantity;

        @Version
        private short version;

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

        public short getVersion() {
            return version;
        }

        public Product setVersion(short version) {
            this.version = version;
            return this;
        }
    }
}
