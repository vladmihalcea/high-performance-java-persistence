package com.vladmihalcea.book.hpjp.spring.transaction.jta.dao;

import com.vladmihalcea.book.hpjp.hibernate.transaction.forum.Post;

/**
 * @author Vlad Mihalcea
 */
public interface PostBatchDAO extends GenericDAO<Post, Long> {

    void savePosts();
}
