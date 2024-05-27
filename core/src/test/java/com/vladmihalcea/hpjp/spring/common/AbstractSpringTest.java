package com.vladmihalcea.hpjp.spring.common;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaDelete;
import jakarta.persistence.criteria.Root;
import org.junit.Before;
import org.junit.runner.RunWith;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.transaction.support.TransactionTemplate;

/**
 * @author Vlad Mihalcea
 */
@RunWith(SpringJUnit4ClassRunner.class)
public abstract class AbstractSpringTest {

    protected final Logger LOGGER = LoggerFactory.getLogger(getClass());

    @Autowired
    protected TransactionTemplate transactionTemplate;

    @PersistenceContext
    protected EntityManager entityManager;

    @Before
    public void init() {
        transactionTemplate.execute(transactionStatus -> {
            CriteriaBuilder builder = entityManager.getCriteriaBuilder();

            for(Class entityClass : entities()) {
                CriteriaDelete criteria = builder.createCriteriaDelete(entityClass);
                criteria.from(entityClass);
                entityManager.createQuery(criteria).executeUpdate();
            }
            return null;
        });
        afterInit();
    }

    protected abstract Class<?>[] entities();

    protected void afterInit() {
    }
}

