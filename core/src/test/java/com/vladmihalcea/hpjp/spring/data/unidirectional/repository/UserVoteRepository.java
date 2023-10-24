package com.vladmihalcea.hpjp.spring.data.unidirectional.repository;

import com.vladmihalcea.hpjp.spring.data.unidirectional.domain.UserVote;
import io.hypersistence.utils.spring.repository.BaseJpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

/**
 * @author Vlad Mihalcea
 */
@Repository
public interface UserVoteRepository extends BaseJpaRepository<UserVote, Long> {

    @Query("""
        delete from UserVote
        where comment.id in (
            select id
            from PostComment
            where post.id = :postId
        )
        """)
    @Modifying
    void deleteAllByPostId(@Param("postId") Long postId);
}
