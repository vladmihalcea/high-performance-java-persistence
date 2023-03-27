package com.vladmihalcea.book.hpjp.spring.data.query.fetch.repository;

import com.vladmihalcea.book.hpjp.spring.data.query.fetch.domain.Post;
import io.hypersistence.utils.spring.repository.BaseJpaRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * @author Vlad Mihalcea
 */
@Repository
public interface PostRepository extends BaseJpaRepository<Post, Long>, CustomPostRepository {

    @Query(
        value = """
            select p
            from Post p
            left join fetch p.comments
            where p.title like :titlePattern
            """,
        countQuery = """
            select count(p)
            from Post p
            where p.title like :titlePattern
            """
    )
    Page<Post> findAllByTitleWithCommentsAntiPattern(@Param("titlePattern") String titlePattern, Pageable pageable);

    @Query("""
        select p
        from Post p
        left join fetch p.comments pc
        where p.id in (
            select id
            from (
              select
                 id as id,
                 dense_rank() over (order by createdOn ASC) as ranking
              from Post
              where title like :titlePattern
            ) pr
            where ranking <= :maxCount
        )
        """
    )
    List<Post> findFirstByTitleWithCommentsByTitle(
        @Param("titlePattern") String titlePattern,
        @Param("maxCount") int maxCount
    );

    @Query(
        value = """
            select p.id
            from Post p
            where p.title like :titlePattern
            """,
        countQuery = """
            select count(p)
            from Post p
            where p.title like :titlePattern
            """
    )
    List<Long> findAllPostIdsByTitle(@Param("titlePattern") String titlePattern, Pageable pageable);

    @Query("""
        select p
        from Post p
        left join fetch p.comments
        where p.id in :postIds
        """
    )
    List<Post> findAllByIdWithComments(@Param("postIds") List<Long> postIds);
}
