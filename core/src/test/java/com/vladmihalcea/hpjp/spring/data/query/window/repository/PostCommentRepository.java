package com.vladmihalcea.hpjp.spring.data.query.window.repository;

import com.vladmihalcea.hpjp.spring.data.query.window.domain.Post;
import com.vladmihalcea.hpjp.spring.data.query.window.domain.PostComment;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.ScrollPosition;
import org.springframework.data.domain.Window;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

/**
 * @author Vlad Mihalcea
 */
@Repository
public interface PostCommentRepository extends JpaRepository<PostComment, Long> {

    Window<PostComment> findByPost(
        Post post,
        Pageable pageable,
        ScrollPosition position
    );
}
