package com.vladmihalcea.book.hpjp.hibernate.inheritance.spring.service.sender;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.vladmihalcea.book.hpjp.hibernate.inheritance.spring.model.EmailNotification;
import com.vladmihalcea.book.hpjp.hibernate.inheritance.spring.model.SmsNotification;
import org.springframework.stereotype.Component;

/**
 * @author Vlad Mihalcea
 */
@Component
public class EmailNotificationSender
	implements NotificationSender<EmailNotification> {

	protected final Logger LOGGER = LoggerFactory.getLogger( getClass() );

	@Override
	public Class<EmailNotification> appliesTo() {
		return EmailNotification.class;
	}

	@Override
	public void send(EmailNotification notification) {
		LOGGER.info(
			"Send Email to {} {} via address: {}",
			 notification.getFirstName(),
			 notification.getLastName(),
			 notification.getEmailAddress()
		);
	}
}
