package com.vladmihalcea.hpjp.spring.data.query.multibag.repository;

import com.vladmihalcea.hpjp.spring.data.query.multibag.domain.Post;

import java.util.List;

/**
 * @author Vlad Mihalcea
 */
public interface CustomPostRepository {

    List<Post> findAllWithCommentsAndTags(long minId, long maxId);
}
