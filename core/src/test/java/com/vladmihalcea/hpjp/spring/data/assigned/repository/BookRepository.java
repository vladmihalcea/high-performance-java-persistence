package com.vladmihalcea.hpjp.spring.data.assigned.repository;

import com.vladmihalcea.hpjp.spring.data.assigned.domain.Book;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

/**
 * @author Vlad Mihalcea
 */
@Repository
public interface BookRepository extends JpaRepository<Book, Long> {
}
