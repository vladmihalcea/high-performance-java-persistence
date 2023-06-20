package com.vladmihalcea.hpjp.spring.data.lock.repository;

import com.vladmihalcea.hpjp.spring.data.lock.domain.PostComment;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * @author Vlad Mihalcea
 */
@Repository
public interface PostCommentRepository extends JpaRepository<PostComment, Long> {

    @Query("""
        select pc
        from PostComment pc
        where pc.post.id = :postId
        """)
    @Lock(LockModeType.PESSIMISTIC_READ)
    List<PostComment> lockAllByPostId(@Param("postId") Long postId);
}
