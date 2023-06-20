package com.vladmihalcea.hpjp.spring.transaction.jpa.dao;

import com.vladmihalcea.hpjp.hibernate.transaction.forum.Post;

/**
 * @author Vlad Mihalcea
 */
public interface PostBatchDAO extends GenericDAO<Post, Long> {

    void savePosts();
}
