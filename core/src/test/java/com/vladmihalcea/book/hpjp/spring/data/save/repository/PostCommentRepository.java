package com.vladmihalcea.book.hpjp.spring.data.save.repository;

import com.vladmihalcea.book.hpjp.spring.data.save.domain.PostComment;
import com.vladmihalcea.spring.repository.HibernateRepository;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

/**
 * @author Vlad Mihalcea
 */
@Repository
public interface PostCommentRepository extends JpaRepository<PostComment, Long>,
    HibernateRepository<PostComment> {

}
