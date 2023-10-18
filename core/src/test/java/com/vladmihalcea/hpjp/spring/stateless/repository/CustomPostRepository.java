package com.vladmihalcea.hpjp.spring.stateless.repository;

import java.util.List;

/**
 * @author Vlad Mihalcea
 */
public interface CustomPostRepository<T> {
    <S extends T> List<S> persistAll(Iterable<S> entities);
}
