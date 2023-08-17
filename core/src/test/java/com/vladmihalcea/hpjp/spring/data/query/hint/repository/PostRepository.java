package com.vladmihalcea.hpjp.spring.data.query.hint.repository;

import com.vladmihalcea.hpjp.spring.data.query.hint.domain.Post;
import io.hypersistence.utils.spring.repository.BaseJpaRepository;
import jakarta.persistence.QueryHint;
import org.hibernate.jpa.AvailableHints;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.jpa.repository.QueryHints;
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
        left join fetch p.comments
        where p.id in :postIds
        """
    )
    @QueryHints(
        @QueryHint(name = AvailableHints.HINT_READ_ONLY, value = "true")
    )
    List<Post> findAllByIdWithComments(@Param("postIds") List<Long> postIds);
}
