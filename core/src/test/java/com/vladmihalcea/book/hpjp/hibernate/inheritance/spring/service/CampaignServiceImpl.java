package com.vladmihalcea.book.hpjp.hibernate.inheritance.spring.service;

import com.vladmihalcea.book.hpjp.hibernate.inheritance.spring.dao.SubscriberDAO;
import com.vladmihalcea.book.hpjp.hibernate.inheritance.spring.model.Subscriber;
import com.vladmihalcea.book.hpjp.hibernate.inheritance.spring.service.sender.CampaignSender;
import jakarta.transaction.Transactional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @author Vlad Mihalcea
 */
@Service
public class CampaignServiceImpl
        implements CampaignService {

    @Autowired
    private SubscriberDAO subscriberDAO;

    @Autowired
    private List<CampaignSender> campaignSenders;

    private Map<Class<? extends Subscriber>, CampaignSender>
            campaignSenderMap = new HashMap<>();

    @PostConstruct
    @SuppressWarnings("unchecked")
    public void init() {
        for (CampaignSender campaignSender : campaignSenders) {
            campaignSenderMap.put(
                    campaignSender.appliesTo(),
                    campaignSender
            );
        }
    }

    @Override
    @Transactional
    @SuppressWarnings("unchecked")
    public void send(String title, String message) {
        List<Subscriber> subscribers = subscriberDAO.findAll();

        for (Subscriber subscriber : subscribers) {
            campaignSenderMap
                    .get(subscriber.getClass())
                    .send(title, message, subscriber);
        }
    }
}
