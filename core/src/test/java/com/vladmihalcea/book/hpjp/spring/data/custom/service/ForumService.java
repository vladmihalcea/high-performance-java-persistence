package com.vladmihalcea.book.hpjp.spring.data.custom.service;

import com.vladmihalcea.book.hpjp.hibernate.forum.Post;
import com.vladmihalcea.book.hpjp.hibernate.query.dto.projection.transformer.PostDTO;

import java.util.List;

/**
 * @author Vlad Mihalcea
 */
public interface ForumService {

    Post findById(Long id);

    List<PostDTO> findPostDTOWithComments();

    void saveAntiPattern(Long postId, String postTitle);
}
