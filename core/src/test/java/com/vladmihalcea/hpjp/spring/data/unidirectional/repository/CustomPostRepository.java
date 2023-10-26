package com.vladmihalcea.hpjp.spring.data.unidirectional.repository;

/**
 * @author Vlad Mihalcea
 */
public interface CustomPostRepository<ID> {

    void deleteById(ID postId);
}
