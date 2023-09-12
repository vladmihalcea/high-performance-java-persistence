package com.vladmihalcea.hpjp.spring.blaze.repository;

import com.vladmihalcea.hpjp.spring.blaze.domain.Post;
import io.hypersistence.utils.spring.repository.BaseJpaRepository;
import org.springframework.stereotype.Repository;

/**
 * @author Vlad Mihalcea
 */
@Repository
public interface PostRepository extends BaseJpaRepository<Post, Long>, CustomPostRepository {
}
