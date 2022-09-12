package com.vladmihalcea.book.hpjp.spring.data.custom.repository;

import com.vladmihalcea.book.hpjp.hibernate.forum.Post;
import com.vladmihalcea.spring.repository.HibernateRepository;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * @author Vlad Mihalcea
 */
@Repository
public interface PostRepository extends
    //HibernateRepository<Post>,
    JpaRepository<Post, Long>, CustomPostRepository {
}
