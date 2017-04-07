package com.vladmihalcea.book.hpjp.util.transaction;

import java.util.function.Function;

import org.hibernate.Session;

/**
 * @author Vlad Mihalcea
 */
@FunctionalInterface
public interface HibernateTransactionFunction<T> extends Function<Session, T> {
	default void beforeTransactionCompletion() {

	}

	default void afterTransactionCompletion() {

	}
}
