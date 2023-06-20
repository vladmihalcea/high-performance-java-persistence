package com.vladmihalcea.hpjp.hibernate.inheritance.spring.service.sender;

import com.vladmihalcea.hpjp.hibernate.inheritance.spring.model.Subscriber;

/**
 * @author Vlad Mihalcea
 */
public interface CampaignSender<S extends Subscriber> {

    Class<S> appliesTo();

    void send(String title, String message, S subscriber);
}
