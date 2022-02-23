package com.vladmihalcea.book.hpjp.spring.data.custom.repository;

import com.vladmihalcea.book.hpjp.hibernate.query.dto.projection.transformer.PostDTO;

import java.util.List;

/**
 * @author Vlad Mihalcea
 */
public interface CustomPostRepository {

    List<PostDTO> findPostDTOWithComments();
}
