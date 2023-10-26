package com.vladmihalcea.hpjp.spring.data.unidirectional.repository;

import com.vladmihalcea.hpjp.spring.data.unidirectional.domain.PostDetails;
import io.hypersistence.utils.spring.repository.BaseJpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

/**
 * @author Vlad Mihalcea
 */
@Repository
public interface PostDetailsRepository extends BaseJpaRepository<PostDetails, Long> {

    @Query("""
        delete from PostDetails
        where post.id = :postId
        """)
    @Modifying
    void deleteByPostId(@Param("postId") Long postId);
}
