package com.vladmihalcea.book.hpjp.hibernate.inheritance.spring.dao;

import org.hibernate.criterion.Order;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Root;
import java.io.Serializable;
import java.util.List;

/**
 * @author Vlad Mihalcea
 */
@Repository
@Transactional
public abstract class GenericDAOImpl<T, ID extends Serializable> implements GenericDAO<T, ID> {

    private final Class<T> entityClass;
    @PersistenceContext
    private EntityManager entityManager;

    protected GenericDAOImpl(Class<T> entityClass) {
        this.entityClass = entityClass;
    }

    protected EntityManager getEntityManager() {
        return entityManager;
    }

    @Override
    public T findById(ID id) {
        return entityManager.find(entityClass, id);
    }

    @Override
    public List<T> findAll() {
        CriteriaBuilder builder = entityManager.getCriteriaBuilder();
        CriteriaQuery<T> criteria = builder.createQuery(entityClass);
        Root<T> root = criteria.from(entityClass);
        criteria.orderBy(builder.asc(root.get("id")));

        return entityManager.createQuery(criteria).getResultList();
    }

    @Override
    public T persist(T entity) {
        entityManager.persist(entity);
        return entity;
    }
}
