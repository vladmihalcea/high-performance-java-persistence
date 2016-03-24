package com.vladmihalcea.book.hpjp.hibernate.transaction.spring.jpa.dao;

import com.vladmihalcea.book.hpjp.hibernate.transaction.forum.Tag;

import java.util.List;

/**
 * <code>TagDAO</code> - Tag DAO
 *
 * @author Vlad Mihalcea
 */
public interface TagDAO extends GenericDAO<Tag, Long> {

    List<Tag> findByName(String... tags);
}
