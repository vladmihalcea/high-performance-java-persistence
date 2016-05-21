package com.vladmihalcea.book.hpjp.hibernate.concurrency;

import com.vladmihalcea.book.hpjp.util.AbstractTest;
import org.hibernate.Session;
import org.hibernate.annotations.DynamicUpdate;
import org.hibernate.annotations.OptimisticLockType;
import org.hibernate.annotations.OptimisticLocking;
import org.hibernate.annotations.SelectBeforeUpdate;
import org.junit.Before;
import org.junit.Test;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import java.math.BigDecimal;

/**
 * OptimisticLockingVersionlessTest - Test to check optimistic checking using the dirty properties instead of a synthetic version column
 *
 * @author Carol Mihalcea
 */
public class OptimisticLockingVersionlessTest extends AbstractTest {

    private Product product;

    @Before
    public void init() {
        super.init();
        product = doInJPA(entityManager -> {
            entityManager.createQuery("delete from Product").executeUpdate();
            Product _product = new Product();
            _product.setId(1L);
            _product.setName("TV");
            _product.setDescription("Plasma TV");
            _product.setPrice(BigDecimal.valueOf(199.99));
            _product.setQuantity(7L);
            entityManager.persist(_product);
            return _product;
        });
    }

    @Test
    public void testVersionlessOptimisticLockingWhenMerging() {

        doInJPA(entityManager -> {
            Product _product = (Product) entityManager.find(Product.class, 1L);
            _product.setPrice(BigDecimal.valueOf(21.22));
            LOGGER.info("Updating product price to {}", _product.getPrice());
        });

        product.setPrice(BigDecimal.ONE);
        doInJPA(entityManager -> {
            LOGGER.info("Merging product, price to be saved is {}", product.getPrice());
            entityManager.merge(product);
            entityManager.flush();
        });
    }

    @Test
    public void testVersionlessOptimisticLockingWhenReattaching() {

        doInJPA(entityManager -> {
            Product _product = (Product) entityManager.find(Product.class, 1L);
            _product.setPrice(BigDecimal.valueOf(21.22));
            LOGGER.info("Updating product price to {}", _product.getPrice());
        });

        product.setPrice(BigDecimal.TEN);
        doInJPA(entityManager -> {
            Session session = entityManager.unwrap(Session.class);
            LOGGER.info("Reattaching product, price to be saved is {}", product.getPrice());
            session.saveOrUpdate(product);
            entityManager.flush();
        });
    }

    @Override
    protected Class<?>[] entities() {
        return new Class<?>[]{
                Product.class
        };
    }

    @Entity(name = "Product")
    @OptimisticLocking(type = OptimisticLockType.DIRTY)
    @DynamicUpdate
    @SelectBeforeUpdate(value = false)
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
