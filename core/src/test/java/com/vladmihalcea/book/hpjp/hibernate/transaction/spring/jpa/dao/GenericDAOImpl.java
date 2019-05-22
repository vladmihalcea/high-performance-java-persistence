package com.vladmihalcea.book.hpjp.hibernate.transaction.spring.jpa.dao;

import com.vladmihalcea.book.hpjp.util.StackTraceUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import java.io.Serializable;

/**
 * @author Vlad Mihalcea
 */
@Repository
@Transactional
public abstract class GenericDAOImpl<T, ID extends Serializable> implements GenericDAO<T, ID> {

    private static final Logger LOGGER = LoggerFactory.getLogger(GenericDAOImpl.class);

    @PersistenceContext
    private EntityManager entityManager;

    private final Class<T> entityClass;

    protected EntityManager getEntityManager() {
        return entityManager;
    }

    protected GenericDAOImpl(Class<T> entityClass) {
        this.entityClass = entityClass;
    }

    public Class<T> getEntityClass() {
        return entityClass;
    }

    @Override
    public T findById(ID id) {
        logCallStack();

        return entityManager.find(entityClass, id);
    }

    @Override
    public T persist(T entity) {
        logCallStack();

        entityManager.persist(entity);
        return entity;
    }

    private void logCallStack() {
        LOGGER.info("Call stack: {}", StackTraceUtils.filter("com.vladmihalcea.book.hpjp.hibernate.transaction"));
    }
}
