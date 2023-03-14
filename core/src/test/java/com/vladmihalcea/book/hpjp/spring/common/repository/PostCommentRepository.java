package com.vladmihalcea.book.hpjp.spring.common.repository;

import com.vladmihalcea.book.hpjp.spring.common.domain.PostComment;
import io.hypersistence.utils.spring.repository.BaseJpaRepository;
import org.springframework.stereotype.Repository;

/**
 * @author Vlad Mihalcea
 */
@Repository
public interface PostCommentRepository extends BaseJpaRepository<PostComment, Long> {

}
