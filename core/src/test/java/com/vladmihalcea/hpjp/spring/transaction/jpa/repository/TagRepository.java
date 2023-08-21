package com.vladmihalcea.hpjp.spring.transaction.jpa.repository;

import com.vladmihalcea.hpjp.hibernate.transaction.forum.Tag;
import io.hypersistence.utils.spring.repository.BaseJpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * @author Vlad Mihalcea
 */
@Repository
public interface TagRepository extends BaseJpaRepository<Tag, Long> {

    @Query("""
        select t
        from Tag t
        where t.name in :tags
        """
    )
    List<Tag> findByName(@Param("tags") String... tags);
}
