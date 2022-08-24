package com.vladmihalcea.book.hpjp.spring.data.projection.repository;

import com.vladmihalcea.book.hpjp.hibernate.query.dto.projection.transformer.PostDTO;
import org.springframework.data.repository.query.Param;

import java.util.List;

/**
 * @author Vlad Mihalcea
 */
public interface CustomPostRepository {

    List<PostDTO> findPostDTOByTitle(@Param("postTitle") String postTitle);
}
