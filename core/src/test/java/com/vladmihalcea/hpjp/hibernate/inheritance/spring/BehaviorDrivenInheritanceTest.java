package com.vladmihalcea.hpjp.hibernate.inheritance.spring;

import com.vladmihalcea.hpjp.hibernate.inheritance.spring.config.BehaviorDrivenInheritanceConfiguration;
import com.vladmihalcea.hpjp.hibernate.inheritance.spring.model.EmailSubscriber;
import com.vladmihalcea.hpjp.hibernate.inheritance.spring.model.SmsSubscriber;
import com.vladmihalcea.hpjp.hibernate.inheritance.spring.model.Subscriber;
import com.vladmihalcea.hpjp.hibernate.inheritance.spring.service.CampaignService;
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

import java.util.List;

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

    @Test
    public void testStaticTypeHandlingIfElse() {
        List<Subscriber> subscribers = entityManager.createQuery("""
            select s 
            from Subscriber s
            order by s.id
            """)
        .getResultList();

        subscribers.stream()
        .forEach(sub -> {
            if(sub instanceof EmailSubscriber emailSub) {
                LOGGER.info("Send email to address [{}]", emailSub.getEmailAddress());
            } else if(sub instanceof SmsSubscriber smsSub) {
                LOGGER.info("Send SMS to phone number [{}]", smsSub.getPhoneNumber());
            } else {
                throw new IllegalStateException(
                    String.format("The [%s] type is not supported!", sub.getClass())
                );
            }
        });
    }

    @Test
    public void testStaticTypeHandlingSwitch() {
        List<Subscriber> subscribers = entityManager.createQuery("""
            select s 
            from Subscriber s
            order by s.id
            """, Subscriber.class)
        .getResultList();

        //Preview feature - requires Java 19 language preview
        /*subscribers.stream()
        .forEach(sub -> {
            switch (sub) {
                case EmailSubscriber emailSub ->
                    LOGGER.info("Send email to address [{}]", emailSub.getEmailAddress());
                case SmsSubscriber smsSub ->
                    LOGGER.info("Send SMS to phone number [{}]", smsSub.getPhoneNumber());
                default -> throw new IllegalStateException(
                    String.format(
                        "The [%s] type is not supported!",
                        sub.getClass()
                    )
                );
            }
        });*/
    }
}
