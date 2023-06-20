package com.vladmihalcea.hpjp.spring.data.query.exists.repository;

import com.vladmihalcea.hpjp.spring.data.query.exists.domain.Post;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * @author Vlad Mihalcea
 */
@Repository
public interface PostRepository extends JpaRepository<Post, Long> {

    Optional<Post> findBySlug(String slug);

    boolean existsById(Long id);

    boolean existsBySlug(String slug);

    @Query(value = """
        SELECT 
            CASE WHEN EXISTS (
                SELECT 1 
                FROM post 
                WHERE slug = :slug
            ) 
            THEN 'true' 
            ELSE 'false'
            END
        """,
        nativeQuery = true
    )
    boolean existsBySlugWithCase(@Param("slug") String slug);

    @Query(value = """
        select count(p.id) = 1 
        from Post p
        where p.slug = :slug
        """
    )
    boolean existsBySlugWithCount(@Param("slug") String slug);
}
