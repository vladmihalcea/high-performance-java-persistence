package com.vladmihalcea.hpjp.spring.data.assigned.repository;

import com.vladmihalcea.hpjp.spring.data.assigned.domain.Book;
import io.hypersistence.utils.spring.repository.BaseJpaRepository;
import org.springframework.stereotype.Repository;

/**
 * @author Vlad Mihalcea
 */
@Repository
public interface BookBaseJpaRepository extends BaseJpaRepository<Book, Long> {
}
