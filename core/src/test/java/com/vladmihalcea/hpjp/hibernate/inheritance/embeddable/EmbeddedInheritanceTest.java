package com.vladmihalcea.hpjp.hibernate.inheritance.embeddable;

import com.vladmihalcea.hpjp.util.AbstractTest;
import com.vladmihalcea.hpjp.util.providers.Database;
import jakarta.persistence.*;
import org.hibernate.annotations.CreationTimestamp;
import org.junit.Test;

import java.io.Serializable;
import java.util.*;

import static java.util.stream.Collectors.groupingBy;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 * @author Vlad Mihalcea
 */
public class EmbeddedInheritanceTest extends AbstractTest {

    @Override
    protected Class<?>[] entities() {
        return new Class<?>[]{
            Subscriber.class,
            Subscription.class,
            EmailSubscription.class,
            SmsSubscription.class,
        };
    }

    @Override
    protected Database database() {
        return Database.POSTGRESQL;
    }

    @Test
    public void test() {
        doInJPA(entityManager -> {
            entityManager.persist(
                new Subscriber()
                    .setId(1L)
                    .setFirstName("Vlad")
                    .setLastName("Mihalcea")
                    .addSubscription(
                        new EmailSubscription()
                            .setOptIn(true)
                            .setEmailAddress("vm@acme.com")
                    )
                    .addSubscription(
                        new SmsSubscription()
                            .setOptIn(true)
                            .setPhoneNumber(123_456_7890L)
                    )
            );
        });

        doInJPA(entityManager -> {
            Subscriber subscriber = entityManager.createQuery("""
                select s
                from Subscriber s
                left join fetch s.subscriptions
                where s.id =:id
                """, Subscriber.class)
            .setParameter("id", 1L)
            .getSingleResult();

            assertEquals(2, subscriber.getSubscriptions().size());
            Map<Class, List<Object>> subscriptionMap = subscriber
                .getSubscriptions()
                .stream()
                .collect(groupingBy(Object::getClass));

            EmailSubscription emailSubscription = (EmailSubscription)
                subscriptionMap.get(EmailSubscription.class).get(0);
            assertEquals(
                "vm@acme.com", emailSubscription.getEmailAddress()
            );

            SmsSubscription smsSubscription = (SmsSubscription)
                subscriptionMap.get(SmsSubscription.class).get(0);
            assertEquals(
                123_456_7890L, smsSubscription.getPhoneNumber().longValue()
            );
        });
    }

    @Entity(name = "Subscriber")
    @Table(name = "subscriber")
    public static class Subscriber {

        @Id
        private Long id;

        @Column(name = "first_name")
        private String firstName;

        @Column(name = "last_name")
        private String lastName;

        @Temporal(TemporalType.TIMESTAMP)
        @CreationTimestamp
        @Column(name = "created_on")
        private Date createdOn;

        @ElementCollection
        @CollectionTable(name = "subscriptions", joinColumns = @JoinColumn(name = "parent_id"))
        private Set<Subscription> subscriptions = new HashSet<>();

        public Long getId() {
            return id;
        }

        public Subscriber setId(Long id) {
            this.id = id;
            return this;
        }

        public String getFirstName() {
            return firstName;
        }

        public Subscriber setFirstName(String firstName) {
            this.firstName = firstName;
            return this;
        }

        public String getLastName() {
            return lastName;
        }

        public Subscriber setLastName(String lastName) {
            this.lastName = lastName;
            return this;
        }

        public Date getCreatedOn() {
            return createdOn;
        }

        public Subscriber setCreatedOn(Date createdOn) {
            this.createdOn = createdOn;
            return this;
        }

        public Set<Subscription> getSubscriptions() {
            return subscriptions;
        }

        public Subscriber addSubscription(Subscription subscription) {
            subscriptions.add(subscription);
            return this;
        }
    }

    @Embeddable
    @DiscriminatorColumn(name = "subscription_type")
    public static class Subscription<T extends Subscription<T>> {

        @Column(name = "opt_in")
        private boolean optIn;

        public boolean isOptIn() {
            return optIn;
        }

        public T setOptIn(boolean optIn) {
            this.optIn = optIn;
            return (T) this;
        }
    }

    @Embeddable
    @DiscriminatorValue("email")
    public static class EmailSubscription extends Subscription<EmailSubscription> {

        @Column(name = "email_address")
        private String emailAddress;

        public String getEmailAddress() {
            return emailAddress;
        }

        public EmailSubscription setEmailAddress(String emailAddress) {
            this.emailAddress = emailAddress;
            return this;
        }
    }

    @Embeddable
    @DiscriminatorValue("sms")
    public static class SmsSubscription extends Subscription<SmsSubscription> {

        @Column(name = "phone_number")
        private Long phoneNumber;

        public Long getPhoneNumber() {
            return phoneNumber;
        }

        public SmsSubscription setPhoneNumber(Long phoneNumber) {
            this.phoneNumber = phoneNumber;
            return this;
        }
    }
}
