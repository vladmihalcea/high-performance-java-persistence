package com.vladmihalcea.hpjp.spring.data.query.multibag.repository;

import com.vladmihalcea.hpjp.spring.data.query.multibag.domain.PostComment;
import io.hypersistence.utils.spring.repository.BaseJpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * @author Vlad Mihalcea
 */
@Repository
public interface PostCommentRepository extends BaseJpaRepository<PostComment, Long> {

    List<PostComment> findAllByReview(String review);
}
