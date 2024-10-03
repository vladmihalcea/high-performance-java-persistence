package com.vladmihalcea.hpjp.spring.transaction.jta.atomikos.dao;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.springframework.stereotype.Repository;

import java.io.Serializable;

/**
 * @author Vlad Mihalcea
 */
@Repository
public abstract class GenericDAOImpl<T, ID extends Serializable> implements GenericDAO<T, ID> {

    @PersistenceContext
    private EntityManager entityManager;

    private final Class<T> entityClass;

    protected EntityManager getEntityManager() {
        return entityManager;
    }

    protected GenericDAOImpl(Class<T> entityClass) {
        this.entityClass = entityClass;
    }

    @Override
    public T findById(ID id) {
        return entityManager.find(entityClass, id);
    }

    @Override
    public T persist(T entity) {
        entityManager.persist(entity);
        return entity;
    }
}
