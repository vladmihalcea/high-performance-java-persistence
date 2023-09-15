package com.vladmihalcea.hpjp.spring.data.cascade.repository;

import com.vladmihalcea.hpjp.spring.data.cascade.domain.Post;
import com.vladmihalcea.hpjp.spring.data.cascade.domain.PostComment;
import io.hypersistence.utils.spring.repository.BaseJpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * @author Vlad Mihalcea
 */
@Repository
public interface PostCommentRepository extends BaseJpaRepository<PostComment, Long> {

    @Query("""
        select pc
        from PostComment pc
        join fetch pc.post p
        join fetch p.comments
        where p.title like :titlePrefix
        """)
    List<PostComment> findAllWithPostTitleLike(@Param("titlePrefix") String titlePrefix);

    @Query("""
        select pc
        from PostComment pc
        join fetch pc.post p
        join fetch p.details d
        where pc.id between :minId and :maxId
        """)
    List<PostComment> findAllWithPostAndDetailsByIds(
        @Param("minId") Long minId,
        @Param("maxId") Long maxId
    );

    @Modifying
    @Query("""
        delete from PostComment c
        where c.post in :posts
        """)
    void deleteAllByPost(@Param("posts") List<Post> posts);
}
