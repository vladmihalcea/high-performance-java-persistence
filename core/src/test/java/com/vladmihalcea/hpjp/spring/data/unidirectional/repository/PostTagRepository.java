package com.vladmihalcea.hpjp.spring.data.unidirectional.repository;

import com.vladmihalcea.hpjp.spring.data.unidirectional.domain.PostTag;
import com.vladmihalcea.hpjp.spring.data.unidirectional.domain.PostTagId;
import io.hypersistence.utils.spring.repository.BaseJpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

/**
 * @author Vlad Mihalcea
 */
@Repository
public interface PostTagRepository extends BaseJpaRepository<PostTag, PostTagId> {

    @Query("""
        delete from PostTag
        where post.id = :postId
        """)
    @Modifying
    void deleteAllByPostId(@Param("postId") Long postId);
}
