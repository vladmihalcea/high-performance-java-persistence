package com.vladmihalcea.hpjp.util.transaction;

import org.hibernate.StatelessSession;

import java.util.function.Consumer;

/**
 * @author Vlad Mihalcea
 */
@FunctionalInterface
public interface HibernateStatelessTransactionConsumer extends Consumer<StatelessSession> {
	default void beforeTransactionCompletion() {

	}

	default void afterTransactionCompletion() {

	}
}
