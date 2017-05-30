package com.vladmihalcea.book.hpjp.hibernate.inheritance.spring.service.sender;

import com.vladmihalcea.book.hpjp.hibernate.inheritance.spring.model.Notification;

/**
 * @author Vlad Mihalcea
 */
public interface NotificationSender<N extends Notification> {

	Class<N> appliesTo();

	void send(N notification);
}
