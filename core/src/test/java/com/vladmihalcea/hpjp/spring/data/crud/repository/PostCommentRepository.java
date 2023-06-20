package com.vladmihalcea.hpjp.spring.data.crud.repository;

import com.vladmihalcea.hpjp.spring.data.crud.domain.PostComment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

/**
 * @author Vlad Mihalcea
 */
@Repository
public interface PostCommentRepository extends JpaRepository<PostComment, Long> {

}
