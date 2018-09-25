package com.vladmihalcea.book.hpjp.hibernate.transaction.spring.jpa.dao;

import com.vladmihalcea.book.hpjp.hibernate.transaction.forum.Post;

import java.util.List;

/**
 * @author Vlad Mihalcea
 */
public interface PostDAO extends GenericDAO<Post, Long> {

    List<Post> findByTitle(String title);
}
