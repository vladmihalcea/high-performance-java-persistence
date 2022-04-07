package com.vladmihalcea.book.hpjp.hibernate.inheritance.spring;

import com.vladmihalcea.book.hpjp.hibernate.inheritance.spring.config.BehaviorDrivenInheritanceConfiguration;
import com.vladmihalcea.book.hpjp.hibernate.inheritance.spring.model.EmailSubscriber;
import com.vladmihalcea.book.hpjp.hibernate.inheritance.spring.model.SmsSubscriber;
import com.vladmihalcea.book.hpjp.hibernate.inheritance.spring.service.CampaignService;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.transaction.TransactionException;
import org.springframework.transaction.support.TransactionCallback;
import org.springframework.transaction.support.TransactionTemplate;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;

/**
 * @author Vlad Mihalcea
 */
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(classes = BehaviorDrivenInheritanceConfiguration.class)
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
public class BehaviorDrivenInheritanceTest {

    protected final Logger LOGGER = LoggerFactory.getLogger(getClass());

    @Autowired
    private TransactionTemplate transactionTemplate;

    @PersistenceContext
    private EntityManager entityManager;

    @Autowired
    private CampaignService campaignService;

    @Test
    public void test() {
        try {
            transactionTemplate.execute((TransactionCallback<Void>) transactionStatus -> {
                EmailSubscriber email = new EmailSubscriber();
                email.setEmailAddress("vlad@acme.com");
                email.setFirstName("Vlad");
                email.setLastName("Mihalcea");

                entityManager.persist(email);

                SmsSubscriber sms = new SmsSubscriber();
                sms.setPhoneNumber("012-345-67890");
                sms.setFirstName("Vlad");
                sms.setLastName("Mihalcea");

                entityManager.persist(sms);

                return null;
            });
        } catch (TransactionException e) {
            LOGGER.error("Failure", e);
        }

        campaignService.send("Black Friday", "High-Performance Java Persistence is 40% OFF");
    }
}
