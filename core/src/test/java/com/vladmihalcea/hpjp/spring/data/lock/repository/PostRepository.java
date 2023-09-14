package com.vladmihalcea.hpjp.spring.data.lock.repository;

import com.vladmihalcea.hpjp.spring.data.lock.domain.Post;
import io.hypersistence.utils.spring.repository.BaseJpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

/**
 * @author Vlad Mihalcea
 */
@Repository
public interface PostRepository extends BaseJpaRepository<Post, Long> {

    @Query("""
        select p
        from Post p
        where p.slug = :slug
        """)
    Post findBySlug(@Param("slug") String slug);
}
