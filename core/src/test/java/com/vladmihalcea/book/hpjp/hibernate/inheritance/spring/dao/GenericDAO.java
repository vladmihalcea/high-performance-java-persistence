package com.vladmihalcea.book.hpjp.hibernate.inheritance.spring.dao;

import java.io.Serializable;
import java.util.List;

/**
 * @author Vlad Mihalcea
 */
public interface GenericDAO<T, ID extends Serializable> {

	T findById(ID id);

	List<T> findAll();

	T persist(T entity);
}
