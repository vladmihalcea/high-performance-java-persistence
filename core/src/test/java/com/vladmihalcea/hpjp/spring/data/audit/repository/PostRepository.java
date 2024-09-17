package com.vladmihalcea.hpjp.spring.data.audit.repository;

import com.vladmihalcea.hpjp.spring.data.audit.domain.Post;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.repository.history.RevisionRepository;
import org.springframework.stereotype.Repository;

/**
 * @author Vlad Mihalcea
 */
@Repository
public interface PostRepository extends JpaRepository<Post, Long>,
    RevisionRepository<Post, Long, Long> {

}
