package com.vladmihalcea.book.hpjp.spring.batch.repository;

import com.vladmihalcea.book.hpjp.spring.batch.domain.Post;
import io.hypersistence.utils.spring.repository.BaseJpaRepository;
import org.springframework.stereotype.Repository;

/**
 * @author Vlad Mihalcea
 */
@Repository
public interface PostRepository extends BaseJpaRepository<Post, Long> {
}
