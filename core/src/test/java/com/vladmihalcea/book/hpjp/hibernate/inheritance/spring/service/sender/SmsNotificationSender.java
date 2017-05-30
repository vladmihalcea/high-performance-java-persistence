package com.vladmihalcea.book.hpjp.hibernate.inheritance.spring.service.sender;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.vladmihalcea.book.hpjp.hibernate.inheritance.spring.model.SmsNotification;
import org.springframework.stereotype.Component;

/**
 * @author Vlad Mihalcea
 */
@Component
public class SmsNotificationSender implements NotificationSender<SmsNotification> {

	protected final Logger LOGGER = LoggerFactory.getLogger( getClass() );

	@Override
	public Class<SmsNotification> appliesTo() {
		return SmsNotification.class;
	}

	@Override
	public void send(SmsNotification notification) {
		LOGGER.info( "Send SMS to {} {} via phone number: {}",
					 notification.getFirstName(),
					 notification.getLastName(),
					 notification.getPhoneNumber()
		);
	}
}
