package com.vladmihalcea.hpjp.spring.data.query.method.repository;

import com.vladmihalcea.hpjp.spring.data.query.method.domain.Post;
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

    //This query will throw a MultipleBagFetchException when Spring bootstraps
    /*
    @Query("""
        select p
        from Post p
        left join fetch p.comments
        left join fetch p.tags
        where p.id between :minId and :maxId
        """)
    List<Post> findAllWithCommentsAndTags(@Param("minId") long minId, @Param("maxId") long maxId);
    */

    @Query("""
        select p
        from Post p
        left join fetch p.comments
        where p.id between :minId and :maxId
        """)
    List<Post> findAllWithComments(@Param("minId") long minId, @Param("maxId") long maxId);

    @Query("""
        select p
        from Post p
        left join fetch p.tags
        where p.id between :minId and :maxId
        """)
    List<Post> findAllWithTags(@Param("minId") long minId, @Param("maxId") long maxId);
}
