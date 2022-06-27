package com.vladmihalcea.book.hpjp.spring.data.query.multibag.repository;

import com.vladmihalcea.book.hpjp.spring.data.query.multibag.domain.Post;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * @author Vlad Mihalcea
 */
@Repository
@Transactional(readOnly = true)
public interface PostRepository extends JpaRepository<Post, Long> , CustomPostRepository {

    @Query("""
        select distinct p
        from Post p
        left join fetch p.comments
        where p.id between :minId and :maxId
        """)
    List<Post> findAllWithComments(@Param("minId") long minId, @Param("maxId") long maxId);

    @Query("""
        select distinct p
        from Post p
        left join fetch p.tags
        where p.id between :minId and :maxId
        """)
    List<Post> findAllWithTags(@Param("minId") long minId, @Param("maxId") long maxId);
}
