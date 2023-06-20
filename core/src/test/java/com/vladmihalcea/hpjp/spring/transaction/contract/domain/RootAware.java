package com.vladmihalcea.hpjp.spring.transaction.contract.domain;

/**
 * @author Vlad Mihalcea
 */
public interface RootAware<T> {
    T root();
}
