package com.vladmihalcea.hpjp.spring.data.custom.repository;

import com.vladmihalcea.hpjp.hibernate.forum.Post;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

/**
 * @author Vlad Mihalcea
 */
@Repository
public interface PostRepository extends
    //HibernateRepository<Post>,
    JpaRepository<Post, Long>, CustomPostRepository {
}
