package com.vladmihalcea.hpjp.spring.transaction.jta.narayana.dao;

import java.io.Serializable;

/**
 * @author Vlad Mihalcea
 */
public interface GenericDAO<T, ID extends Serializable> {

    T findById(ID id);

    T persist(T entity);
}
