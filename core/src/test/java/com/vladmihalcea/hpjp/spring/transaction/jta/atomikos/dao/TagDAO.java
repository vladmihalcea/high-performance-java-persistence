package com.vladmihalcea.hpjp.spring.transaction.jta.atomikos.dao;

import com.vladmihalcea.hpjp.hibernate.transaction.forum.Tag;

import java.util.List;

/**
 * @author Vlad Mihalcea
 */
public interface TagDAO extends GenericDAO<Tag, Long> {

    List<Tag> findByName(String... tags);
}
