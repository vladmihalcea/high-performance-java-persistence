package com.vladmihalcea.book.hpjp.spring.transaction.readonly;

import com.vladmihalcea.book.hpjp.spring.transaction.readonly.config.SpringDataJPAReadOnlyConfiguration;
import com.vladmihalcea.book.hpjp.spring.transaction.readonly.domain.Product;
import com.vladmihalcea.book.hpjp.spring.transaction.readonly.repository.ProductRepository;
import com.vladmihalcea.book.hpjp.spring.transaction.readonly.service.ProductService;
import com.vladmihalcea.book.hpjp.spring.transaction.readonly.service.fxrate.FxCurrency;
import jakarta.persistence.EntityManager;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.transaction.TransactionException;
import org.springframework.transaction.support.TransactionCallback;
import org.springframework.transaction.support.TransactionTemplate;

import java.math.BigDecimal;

import static org.junit.Assert.assertEquals;

/**
 * @author Vlad Mihalcea
 */
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(classes = SpringDataJPAReadOnlyConfiguration.class)
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
public class SpringDataJPAReadOnlyTest {

    protected final Logger LOGGER = LoggerFactory.getLogger(getClass());

    @Autowired
    private TransactionTemplate transactionTemplate;

    @Autowired
    private EntityManager entityManager;

    @Autowired
    private ProductService productService;

    @Before
    public void init() {
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
        Product ebook = productService.findById(1L, FxCurrency.EUR);
        assertEquals(FxCurrency.EUR, ebook.getCurrency());
        LOGGER.info("The book price is {} {}", ebook.getPrice(), ebook.getCurrency());
    }

    @Test
    public void testReadWrite() {
        Product ebook = productService.findByIdReadWrite(1L, FxCurrency.EUR);
        assertEquals(FxCurrency.EUR, ebook.getCurrency());
        LOGGER.info("The book price is {} {}", ebook.getPrice(), ebook.getCurrency());
    }
}

