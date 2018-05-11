package com.vladmihalcea.book.hpjp.hibernate.inheritance.spring.service.sender;

import com.vladmihalcea.book.hpjp.hibernate.inheritance.spring.model.SmsSubscriber;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * @author Vlad Mihalcea
 */
@Component
public class SmsCampaignSender implements CampaignSender<SmsSubscriber> {

    protected final Logger LOGGER = LoggerFactory.getLogger(getClass());

    @Override
    public Class<SmsSubscriber> appliesTo() {
        return SmsSubscriber.class;
    }

    @Override
    public void send(String title, String message, SmsSubscriber subscriber) {
        LOGGER.info("Send SMS: {} - {} to phone number: {}",
                title,
                message,
                subscriber.getPhoneNumber()
        );
    }
}
