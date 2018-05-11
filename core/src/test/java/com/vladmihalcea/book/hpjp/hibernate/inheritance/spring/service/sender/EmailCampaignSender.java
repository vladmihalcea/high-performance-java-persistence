package com.vladmihalcea.book.hpjp.hibernate.inheritance.spring.service.sender;

import com.vladmihalcea.book.hpjp.hibernate.inheritance.spring.model.EmailSubscriber;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * @author Vlad Mihalcea
 */
@Component
public class EmailCampaignSender implements CampaignSender<EmailSubscriber> {

    protected final Logger LOGGER = LoggerFactory.getLogger(getClass());

    @Override
    public Class<EmailSubscriber> appliesTo() {
        return EmailSubscriber.class;
    }

    @Override
    public void send(String title, String message, EmailSubscriber subscriber) {
        LOGGER.info("Send Email: {} - {} to address: {}",
                title,
                message,
                subscriber.getEmailAddress()
        );
    }
}
