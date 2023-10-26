package com.vladmihalcea.hpjp.spring.data.merge.repository;

import com.vladmihalcea.hpjp.spring.data.merge.domain.Post;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * @author Vlad Mihalcea
 */
@Repository
public interface PostRepository extends JpaRepository<Post, Long> {

    @Query("""
        select p
        from Post p
        left join fetch p.comments
        where p.title like :titlePrefix
        """)
    List<Post> findAllWithCommentsByTitleLike(@Param("titlePrefix") String titlePrefix);
}
