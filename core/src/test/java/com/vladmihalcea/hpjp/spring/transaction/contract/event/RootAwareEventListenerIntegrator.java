package com.vladmihalcea.hpjp.spring.transaction.contract.event;

import org.hibernate.boot.Metadata;
import org.hibernate.boot.spi.BootstrapContext;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.event.service.spi.EventListenerRegistry;
import org.hibernate.event.spi.EventType;
import org.hibernate.integrator.spi.Integrator;
import org.hibernate.service.spi.SessionFactoryServiceRegistry;

public class RootAwareEventListenerIntegrator implements Integrator {

    public static final RootAwareEventListenerIntegrator INSTANCE =
        new RootAwareEventListenerIntegrator();

    @Override
    public void integrate(Metadata metadata, BootstrapContext bootstrapContext, SessionFactoryImplementor sessionFactory) {
        final EventListenerRegistry eventListenerRegistry = sessionFactory
            .getServiceRegistry()
            .getService(EventListenerRegistry.class);

        eventListenerRegistry.appendListeners(
            EventType.PERSIST,
            RootAwareInsertEventListener.INSTANCE
        );
        eventListenerRegistry.appendListeners(
            EventType.FLUSH_ENTITY,
            RootAwareUpdateAndDeleteEventListener.INSTANCE
        );
    }

    @Override
    public void disintegrate(
        SessionFactoryImplementor sessionFactory,
        SessionFactoryServiceRegistry serviceRegistry) {

    }
}

