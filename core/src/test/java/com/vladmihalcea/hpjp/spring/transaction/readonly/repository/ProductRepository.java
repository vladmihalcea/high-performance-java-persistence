package com.vladmihalcea.hpjp.spring.transaction.readonly.repository;

import com.vladmihalcea.hpjp.spring.transaction.readonly.domain.Product;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

/**
 * @author Vlad Mihalcea
 */
@Repository
public interface ProductRepository extends JpaRepository<Product, Long> {
}
