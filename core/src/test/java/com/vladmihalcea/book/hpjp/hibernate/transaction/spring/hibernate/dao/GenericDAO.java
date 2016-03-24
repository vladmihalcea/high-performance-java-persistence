package com.vladmihalcea.book.hpjp.hibernate.transaction.spring.hibernate.dao;

import java.io.Serializable;

/**
 * <code>GenericDAO</code> - Generic DAO
 *
 * @author Vlad Mihalcea
 */
public interface GenericDAO<T, ID extends Serializable> {

    T findById(ID id);

    T persist(T entity);
}
