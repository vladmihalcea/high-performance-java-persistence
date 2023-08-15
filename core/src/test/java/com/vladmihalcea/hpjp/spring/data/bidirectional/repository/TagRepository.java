package com.vladmihalcea.hpjp.spring.data.bidirectional.repository;

import com.vladmihalcea.hpjp.spring.data.bidirectional.domain.Tag;
import io.hypersistence.utils.spring.repository.BaseJpaRepository;
import org.springframework.stereotype.Repository;

/**
 * @author Vlad Mihalcea
 */
@Repository
public interface TagRepository extends BaseJpaRepository<Tag, Long> {
}
