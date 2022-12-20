package com.vladmihalcea.book.hpjp.spring.common.repository;

import com.vladmihalcea.book.hpjp.spring.common.domain.Post;
import com.vladmihalcea.book.hpjp.spring.common.domain.PostComment;
import com.vladmihalcea.spring.repository.BaseJpaRepository;
import org.springframework.stereotype.Repository;

/**
 * @author Vlad Mihalcea
 */
@Repository
public interface PostCommentRepository extends BaseJpaRepository<PostComment, Long> {

}
