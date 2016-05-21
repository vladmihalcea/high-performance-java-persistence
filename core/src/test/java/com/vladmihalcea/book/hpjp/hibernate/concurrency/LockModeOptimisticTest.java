package com.vladmihalcea.book.hpjp.hibernate.concurrency;

import org.hibernate.LockMode;
import org.hibernate.LockOptions;
import org.hibernate.Session;
import org.junit.Test;

import javax.persistence.OptimisticLockException;
import java.math.BigDecimal;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotSame;
import static org.junit.Assert.fail;

/**
 * LockModeOptimisticTest - Test to check LockMode.OPTIMISTIC
 *
 * @author Carol Mihalcea
 */
public class LockModeOptimisticTest extends AbstractLockModeOptimisticTest {

    @Test
    public void testImplicitOptimisticLocking() {

        doInJPA(entityManager -> {
            final Product product = (Product) entityManager.find(Product.class, 1L);
            try {
                executeSync(() -> doInJPA(_entityManager -> {
                    Product _product = (Product) _entityManager.find(Product.class, 1L);
                    assertNotSame(product, _product);
                    _product.setPrice(BigDecimal.valueOf(14.49));
                }));
            } catch (Exception e) {
                fail(e.getMessage());
            }
            OrderLine orderLine = new OrderLine(product);
            entityManager.persist(orderLine);
        });
    }

    @Test
    public void testExplicitOptimisticLocking() {

        try {
            doInJPA(entityManager -> {
                Session session = entityManager.unwrap(Session.class);
                final Product product = (Product) session.get(Product.class, 1L, new LockOptions(LockMode.OPTIMISTIC));

                executeSync(() -> {
                    doInJPA(_entityManager -> {
                        Product _product = (Product) _entityManager.find(Product.class, 1L);
                        assertNotSame(product, _product);
                        _product.setPrice(BigDecimal.valueOf(14.49));
                    });
                });

                OrderLine orderLine = new OrderLine(product);
                entityManager.persist(orderLine);
            });
            fail("It should have thrown OptimisticEntityLockException!");
        } catch (Exception expected) {
            assertEquals(OptimisticLockException.class, expected.getCause().getClass());
            LOGGER.info("Failure: ", expected);
        }
    }
}
