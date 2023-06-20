package com.vladmihalcea.hpjp.spring.data.base.service;

import com.vladmihalcea.hpjp.hibernate.forum.Post;

/**
 * @author Vlad Mihalcea
 */
public interface ForumService {

    Post findById(Long id);

    Post createPost(Post post);

    Post updatePost(Post post);
}
