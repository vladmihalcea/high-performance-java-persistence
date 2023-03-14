package com.vladmihalcea.book.hpjp.spring.data.lock.repository;

import com.vladmihalcea.book.hpjp.spring.data.lock.domain.Post;
import io.hypersistence.utils.spring.repository.BaseJpaRepository;
import org.springframework.stereotype.Repository;

/**
 * @author Vlad Mihalcea
 */
@Repository
public interface PostRepository extends BaseJpaRepository<Post, Long> {
}
