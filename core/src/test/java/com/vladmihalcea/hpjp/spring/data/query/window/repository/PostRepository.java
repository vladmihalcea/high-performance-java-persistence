package com.vladmihalcea.hpjp.spring.data.query.window.repository;

import com.vladmihalcea.hpjp.spring.data.query.window.domain.Post;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

/**
 * @author Vlad Mihalcea
 */
@Repository
public interface PostRepository extends JpaRepository<Post, Long> {

}
