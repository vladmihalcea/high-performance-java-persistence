package com.vladmihalcea.hpjp.spring.blaze.repository;

import com.vladmihalcea.hpjp.hibernate.fetching.pagination.Post;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

/**
 * @author Vlad Mihalcea
 */
@Repository
public interface PostRepository extends JpaRepository<Post, Long>, CustomPostRepository {
}
