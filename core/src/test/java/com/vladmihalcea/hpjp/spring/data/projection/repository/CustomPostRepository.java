package com.vladmihalcea.hpjp.spring.data.projection.repository;

import com.vladmihalcea.hpjp.hibernate.query.dto.projection.transformer.PostDTO;
import org.springframework.data.repository.query.Param;

import java.util.List;

/**
 * @author Vlad Mihalcea
 */
public interface CustomPostRepository {

    List<PostDTO> findPostDTOByPostTitle(@Param("postTitle") String postTitle);
}
