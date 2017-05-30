package com.vladmihalcea.book.hpjp.hibernate.inheritance.spring.dao;

import java.io.Serializable;
import java.util.List;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;

import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

/**
 * @author Vlad Mihalcea
 */
@Repository
@Transactional
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
		return entityManager.find( entityClass, id );
	}

	@Override
	public List<T> findAll() {
		CriteriaBuilder builder = entityManager.getCriteriaBuilder();
		CriteriaQuery<T> criteria = builder.createQuery( entityClass );
		criteria.from( entityClass );

		return entityManager.createQuery( criteria ).getResultList();
	}

	@Override
	public T persist(T entity) {
		entityManager.persist( entity );
		return entity;
	}
}
