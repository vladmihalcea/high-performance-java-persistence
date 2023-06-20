package com.vladmihalcea.hpjp.spring.data.custom.service;

import com.vladmihalcea.hpjp.hibernate.forum.Post;
import com.vladmihalcea.hpjp.hibernate.query.dto.projection.transformer.PostDTO;

import java.util.List;

/**
 * @author Vlad Mihalcea
 */
public interface ForumService {

    Post findById(Long id);

    List<PostDTO> findPostDTOWithComments();

    void saveAntiPattern(Long postId, String postTitle);

    Post createPost(Long id, String title);

    void updatePostTitle(Long id, String title);

    void deleteAll();
}
