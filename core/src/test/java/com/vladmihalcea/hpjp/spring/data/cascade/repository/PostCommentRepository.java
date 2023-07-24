package com.vladmihalcea.hpjp.spring.data.cascade.repository;

import com.vladmihalcea.hpjp.spring.data.cascade.domain.PostComment;
import io.hypersistence.utils.spring.repository.BaseJpaRepository;
import org.springframework.stereotype.Repository;

/**
 * @author Vlad Mihalcea
 */
@Repository
public interface PostCommentRepository extends BaseJpaRepository<PostComment, Long> {
}
