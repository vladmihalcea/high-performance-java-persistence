package com.vladmihalcea.hpjp.spring.data.crud.repository;

import com.vladmihalcea.hpjp.spring.data.crud.domain.Post;
import io.hypersistence.utils.spring.repository.HibernateRepository;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

/**
 * @author Vlad Mihalcea
 */
@Repository
public interface DefaultPostRepository extends JpaRepository<Post, Long> {

}
