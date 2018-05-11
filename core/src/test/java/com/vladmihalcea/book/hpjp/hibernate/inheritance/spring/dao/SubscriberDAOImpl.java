package com.vladmihalcea.book.hpjp.hibernate.inheritance.spring.dao;

import com.vladmihalcea.book.hpjp.hibernate.inheritance.spring.model.Subscriber;
import org.springframework.stereotype.Repository;

/**
 * @author Vlad Mihalcea
 */
@Repository
public class SubscriberDAOImpl
        extends GenericDAOImpl<Subscriber, Long>
        implements SubscriberDAO {

    protected SubscriberDAOImpl() {
        super(Subscriber.class);
    }
}
