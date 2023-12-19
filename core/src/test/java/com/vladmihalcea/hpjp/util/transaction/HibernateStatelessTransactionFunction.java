package com.vladmihalcea.hpjp.util.transaction;

import org.hibernate.StatelessSession;

import java.util.function.Function;

/**
 * @author Vlad Mihalcea
 */
@FunctionalInterface
public interface HibernateStatelessTransactionFunction<T> extends Function<StatelessSession, T> {
	default void beforeTransactionCompletion() {

	}

	default void afterTransactionCompletion() {

	}
}
