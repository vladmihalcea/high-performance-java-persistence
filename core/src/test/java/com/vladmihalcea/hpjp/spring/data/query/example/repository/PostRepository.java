package com.vladmihalcea.hpjp.spring.data.query.example.repository;

import com.vladmihalcea.hpjp.spring.data.query.example.domain.Post;
import io.hypersistence.utils.spring.repository.BaseJpaRepository;
import org.springframework.stereotype.Repository;

/**
 * @author Vlad Mihalcea
 */
@Repository
public interface PostRepository extends BaseJpaRepository<Post, Long> {

}
