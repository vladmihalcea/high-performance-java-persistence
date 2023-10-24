package com.vladmihalcea.hpjp.spring.data.unidirectional.event;

import org.hibernate.boot.Metadata;
import org.hibernate.boot.spi.BootstrapContext;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.event.service.spi.EventListenerRegistry;
import org.hibernate.event.spi.EventType;
import org.hibernate.integrator.spi.Integrator;
import org.hibernate.service.spi.SessionFactoryServiceRegistry;

public class CascadeDeleteEventListenerIntegrator implements Integrator {

    public static final CascadeDeleteEventListenerIntegrator INSTANCE =
        new CascadeDeleteEventListenerIntegrator();

    @Override
    public void integrate(Metadata metadata, BootstrapContext bootstrapContext, SessionFactoryImplementor sessionFactory) {
        final EventListenerRegistry eventListenerRegistry = sessionFactory
            .getServiceRegistry()
            .getService(EventListenerRegistry.class);

        eventListenerRegistry.prependListeners(
            EventType.DELETE,
            CascadeDeleteEventListener.INSTANCE
        );
    }

    @Override
    public void disintegrate(
        SessionFactoryImplementor sessionFactory,
        SessionFactoryServiceRegistry serviceRegistry) {
    }
}

