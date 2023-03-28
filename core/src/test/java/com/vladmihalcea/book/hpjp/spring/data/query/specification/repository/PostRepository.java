package com.vladmihalcea.book.hpjp.spring.data.query.specification.repository;

import com.vladmihalcea.book.hpjp.spring.data.query.specification.domain.Post;
import io.hypersistence.utils.spring.repository.BaseJpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * @author Vlad Mihalcea
 */
@Repository
public interface PostRepository extends BaseJpaRepository<Post, Long> {

}
