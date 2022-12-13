package com.vladmihalcea.book.hpjp.spring.transaction.contract.domain;

/**
 * @author Vlad Mihalcea
 */
public interface RootAware<T> {
    T root();
}
