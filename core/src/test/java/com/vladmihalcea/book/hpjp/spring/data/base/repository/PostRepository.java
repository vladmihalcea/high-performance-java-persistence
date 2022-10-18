package com.vladmihalcea.book.hpjp.spring.data.base.repository;

import com.vladmihalcea.book.hpjp.hibernate.forum.Post;
import com.vladmihalcea.book.hpjp.spring.data.custom.repository.CustomPostRepository;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

/**
 * @author Vlad Mihalcea
 */
@Repository
public interface PostRepository extends
    BaseHibernateRepository<Post>,
    JpaRepository<Post, Long> {
}
