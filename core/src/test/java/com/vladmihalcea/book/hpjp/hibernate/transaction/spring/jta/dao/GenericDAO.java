package com.vladmihalcea.book.hpjp.hibernate.transaction.spring.jta.dao;

import java.io.Serializable;

/**
 * @author Vlad Mihalcea
 */
public interface GenericDAO<T, ID extends Serializable> {

    T findById(ID id);

    T persist(T entity);
}
