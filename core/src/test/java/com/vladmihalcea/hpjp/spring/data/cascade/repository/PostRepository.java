package com.vladmihalcea.hpjp.spring.data.cascade.repository;

import com.vladmihalcea.hpjp.spring.data.cascade.domain.Post;
import io.hypersistence.utils.spring.repository.BaseJpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * @author Vlad Mihalcea
 */
@Repository
public interface PostRepository extends BaseJpaRepository<Post, Long> {

    @Query("""
        select p
        from Post p
        left join fetch p.details
        left join fetch p.comments
        where p.title like :titlePrefix
        """)
    List<Post> findAllByTitleLike(@Param("titlePrefix") String titlePrefix);

    @Query("""
        select p
        from Post p
        left join fetch p.details
        left join fetch p.comments
        where p.id = :id
        """)
    Post findByIdWithComments(@Param("id") Long id);
}
