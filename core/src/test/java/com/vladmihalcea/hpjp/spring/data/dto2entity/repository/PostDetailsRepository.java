package com.vladmihalcea.hpjp.spring.data.dto2entity.repository;

import com.vladmihalcea.hpjp.spring.data.dto2entity.domain.PostDetails;
import io.hypersistence.utils.spring.repository.BaseJpaRepository;
import org.springframework.stereotype.Repository;

/**
 * @author Vlad Mihalcea
 */
@Repository
public interface PostDetailsRepository extends BaseJpaRepository<PostDetails, Long> {
}
