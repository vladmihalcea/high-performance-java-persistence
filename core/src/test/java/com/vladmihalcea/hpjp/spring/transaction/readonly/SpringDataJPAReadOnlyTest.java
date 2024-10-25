package com.vladmihalcea.hpjp.spring.transaction.readonly;

import com.vladmihalcea.hpjp.spring.common.AbstractSpringTest;
import com.vladmihalcea.hpjp.spring.transaction.readonly.config.SpringDataJPAReadOnlyConfiguration;
import com.vladmihalcea.hpjp.spring.transaction.readonly.domain.Product;
import com.vladmihalcea.hpjp.spring.transaction.readonly.service.ProductService;
import com.vladmihalcea.hpjp.spring.transaction.readonly.service.fxrate.FxCurrency;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.transaction.TransactionException;
import org.springframework.transaction.support.TransactionCallback;

import java.math.BigDecimal;

import static org.junit.Assert.assertEquals;

/**
 * @author Vlad Mihalcea
 */
@ContextConfiguration(classes = SpringDataJPAReadOnlyConfiguration.class)
public class SpringDataJPAReadOnlyTest extends AbstractSpringTest {

    @Autowired
    private ProductService productService;

    @Override
    protected Class<?>[] entities() {
        return new Class[]{
            Product.class
        };
    }

    @Override
    public void afterInit() {
        try {
            transactionTemplate.execute((TransactionCallback<Void>) transactionStatus -> {
                entityManager.persist(
                    new Product()
                        .setId(1L)
                        .setName("High-Performance Java Persistence eBook")
                        .setPrice(BigDecimal.valueOf(24.9))
                        .setCurrency(FxCurrency.USD)
                );

                entityManager.persist(
                    new Product()
                        .setId(2L)
                        .setName("Hypersistence Optimizer")
                        .setPrice(BigDecimal.valueOf(49))
                        .setCurrency(FxCurrency.USD)
                );
                return null;
            });
        } catch (TransactionException e) {
            LOGGER.error("Failure", e);
        }
    }

    @Test
    public void testReadOnly() {
        Product ebook = productService.getAsCurrency(1L, FxCurrency.EUR);
        assertEquals(FxCurrency.EUR, ebook.getCurrency());
        LOGGER.info("The book price is {} {}", ebook.getPrice(), ebook.getCurrency());
    }

    @Test
    public void testReadWrite() {
        Product ebook = productService.convertToCurrency(1L, FxCurrency.EUR);
        assertEquals(FxCurrency.EUR, ebook.getCurrency());
        LOGGER.info("The book price is {} {}", ebook.getPrice(), ebook.getCurrency());
    }
}

