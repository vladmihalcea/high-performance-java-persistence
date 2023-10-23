package com.vladmihalcea.hpjp.spring.data.query.fetch.repository;

import com.vladmihalcea.hpjp.spring.data.query.fetch.domain.Post;
import io.hypersistence.utils.spring.repository.BaseJpaRepository;
import jakarta.persistence.QueryHint;
import org.hibernate.jpa.AvailableHints;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.jpa.repository.QueryHints;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.stream.Stream;

/**
 * @author Vlad Mihalcea
 */
@Repository
public interface PostRepository extends BaseJpaRepository<Post, Long> {

    Page<Post> findAllByTitleLike(@Param("titlePattern") String titlePattern, Pageable pageRequest);

    @Query("""
        select p
        from Post p
        where p.title like :titlePattern
        """
    )
    Page<Post> findAllByTitleLikeQuery(@Param("titlePattern") String titlePattern, Pageable pageRequest);

    @Query(value = """
        SELECT p.id, p.title, p.created_on
        FROM post p
        WHERE p.title ilike :titlePattern
        ORDER BY p.created_on
        """,
        nativeQuery = true
    )
    Page<Post> findAllByTitleLikeNativeQuery(@Param("titlePattern") String titlePattern, Pageable pageRequest);

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
    Page<Post> findAllByTitleWithCommentsAntiPattern(
        @Param("titlePattern") String titlePattern,
        Pageable pageRequest
    );

    @Query("""
        select p
        from Post p
        left join fetch p.comments pc
        where p.id in (
            select pr.id
            from (
              select
                 p1.id as id,
                 dense_rank() over (order by p1.createdOn, p1.id) as ranking
              from Post p1
              where p1.title like :titlePattern
            ) pr
            where pr.ranking <= :maxCount
        )
        """
    )
    List<Post> findFirstByTitleWithCommentsByTitle(
        @Param("titlePattern") String titlePattern,
        @Param("maxCount") int maxCount
    );

    @Query("""
        select p.id
        from Post p
        where p.title like :titlePattern
        """
    )
    List<Long> findAllPostIdsByTitle(@Param("titlePattern") String titlePattern, Pageable pageRequest);

    @Query("""
        select p
        from Post p
        left join fetch p.comments
        where p.id in :postIds
        """
    )
    List<Post> findAllByIdWithComments(@Param("postIds") List<Long> postIds);

    @Query("""
        select p
        from Post p
        where date(p.createdOn) >= :sinceDate
        """
    )
    @QueryHints(
        @QueryHint(name = AvailableHints.HINT_FETCH_SIZE, value = "25")
    )
    Stream<Post> streamByCreatedOnSince(@Param("sinceDate") LocalDate sinceDate);
}
