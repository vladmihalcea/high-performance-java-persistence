package com.vladmihalcea.book.hpjp.spring.common.repository;

import com.vladmihalcea.book.hpjp.spring.common.domain.Post;
import io.hypersistence.utils.spring.repository.BaseJpaRepository;
import org.springframework.stereotype.Repository;

/**
 * @author Vlad Mihalcea
 */
@Repository
public interface PostRepository extends BaseJpaRepository<Post, Long> {

    Post findBySlug(String slug);
}
