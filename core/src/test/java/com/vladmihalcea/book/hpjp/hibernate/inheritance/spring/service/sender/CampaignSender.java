package com.vladmihalcea.book.hpjp.hibernate.inheritance.spring.service.sender;

import com.vladmihalcea.book.hpjp.hibernate.inheritance.spring.model.Subscriber;

/**
 * @author Vlad Mihalcea
 */
public interface CampaignSender<S extends Subscriber> {

    Class<S> appliesTo();

    void send(String title, String message, S subscriber);
}
