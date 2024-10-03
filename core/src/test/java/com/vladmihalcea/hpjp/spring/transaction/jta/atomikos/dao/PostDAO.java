package com.vladmihalcea.hpjp.spring.transaction.jta.atomikos.dao;

import com.vladmihalcea.hpjp.hibernate.forum.dto.PostDTO;
import com.vladmihalcea.hpjp.hibernate.transaction.forum.Post;

import java.util.List;

/**
 * @author Vlad Mihalcea
 */
public interface PostDAO extends GenericDAO<Post, Long> {

    List<Post> findByTitle(String title);

    PostDTO getPostDTOById(Long id);
}
