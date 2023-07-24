package com.vladmihalcea.hpjp.spring.data.cascade.repository;

import com.vladmihalcea.hpjp.spring.data.cascade.domain.Post;
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
        join fetch p.comments
        where p.id = :id
        """)
    Post findByIdWithComments(@Param("id") Long id);
}
