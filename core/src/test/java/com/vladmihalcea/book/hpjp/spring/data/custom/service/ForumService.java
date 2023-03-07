package com.vladmihalcea.book.hpjp.spring.data.custom.service;

import com.vladmihalcea.book.hpjp.hibernate.forum.Post;
import com.vladmihalcea.book.hpjp.hibernate.query.dto.projection.transformer.PostDTO;
import org.springframework.transaction.annotation.Transactional;

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
