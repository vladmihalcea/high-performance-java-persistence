package com.vladmihalcea.book.hpjp.hibernate.transaction.spring.data.repository;

import com.vladmihalcea.book.hpjp.hibernate.forum.Post;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

/**
 * @author Vlad Mihalcea
 */
@Repository
public interface PostRepository extends JpaRepository<Post, Long>, CustomPostRepository {
}
