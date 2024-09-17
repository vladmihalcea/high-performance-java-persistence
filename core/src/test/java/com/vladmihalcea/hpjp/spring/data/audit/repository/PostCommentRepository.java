package com.vladmihalcea.hpjp.spring.data.audit.repository;

import com.vladmihalcea.hpjp.spring.data.audit.domain.Post;
import com.vladmihalcea.hpjp.spring.data.audit.domain.PostComment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.repository.history.RevisionRepository;
import org.springframework.stereotype.Repository;

/**
 * @author Vlad Mihalcea
 */
@Repository
public interface PostCommentRepository extends JpaRepository<PostComment, Long>,
    RevisionRepository<PostComment, Long, Long> {

    void deleteByPost(Post post);
}
