package com.vladmihalcea.hpjp.hibernate.identifier.batch.jta;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.transaction.support.TransactionCallback;
import org.springframework.transaction.support.TransactionTemplate;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;

@ExtendWith(SpringExtension.class)
@ContextConfiguration(classes = JTATableIdentifierTestConfiguration.class)
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
public class JTATableIdentifierTest {

    protected final Logger LOGGER = LoggerFactory.getLogger(getClass());

    @PersistenceContext
    private EntityManager entityManager;

    @Autowired
    private TransactionTemplate transactionTemplate;

    @Test
    public void testTableIdentifierGenerator() {
        LOGGER.debug("testIdentityIdentifierGenerator");
        transactionTemplate.execute((TransactionCallback<Void>) transactionStatus -> {
            for (int i = 0; i < 5; i++) {
                entityManager.persist(new Post());
            }
            LOGGER.debug("Flush is triggered at commit-time");
            return null;
        });
    }
}
