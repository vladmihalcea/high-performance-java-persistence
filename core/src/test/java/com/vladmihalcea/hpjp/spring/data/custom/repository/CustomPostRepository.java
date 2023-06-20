package com.vladmihalcea.hpjp.spring.data.custom.repository;

import com.vladmihalcea.hpjp.hibernate.forum.Post;
import com.vladmihalcea.hpjp.hibernate.query.dto.projection.transformer.PostDTO;

import java.util.List;

/**
 * @author Vlad Mihalcea
 */
public interface CustomPostRepository {

    List<PostDTO> findPostDTOWithComments();

    List<String> findPostTitleByTags(List<String> tags);

    void deleteAll(List<Post> posts);
}
