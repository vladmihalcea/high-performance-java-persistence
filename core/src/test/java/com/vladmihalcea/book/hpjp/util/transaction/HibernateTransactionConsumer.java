package com.vladmihalcea.book.hpjp.util.transaction;

import java.util.function.Consumer;

import org.hibernate.Session;

/**
 * @author Vlad Mihalcea
 */
@FunctionalInterface
public interface HibernateTransactionConsumer extends Consumer<Session> {
	default void beforeTransactionCompletion() {

	}

	default void afterTransactionCompletion() {

	}
}
