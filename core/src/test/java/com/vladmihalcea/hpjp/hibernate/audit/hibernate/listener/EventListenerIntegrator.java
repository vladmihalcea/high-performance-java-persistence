package com.vladmihalcea.hpjp.hibernate.audit.hibernate.listener;

import org.hibernate.boot.Metadata;
import org.hibernate.boot.spi.BootstrapContext;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.event.service.spi.EventListenerRegistry;
import org.hibernate.event.spi.EventType;
import org.hibernate.integrator.spi.Integrator;
import org.hibernate.service.spi.SessionFactoryServiceRegistry;

/**
 * @author Vlad Mihalcea
 */
public class EventListenerIntegrator implements Integrator {

    public static final EventListenerIntegrator INSTANCE = new EventListenerIntegrator();

    @Override
    public void integrate(Metadata metadata, BootstrapContext bootstrapContext, SessionFactoryImplementor sessionFactory) {
        final EventListenerRegistry eventListenerRegistry = sessionFactory.getServiceRegistry().getService(EventListenerRegistry.class);

        eventListenerRegistry.appendListeners(
            EventType.POST_LOAD,
            AuditLogPostLoadEventListener.INSTANCE
        );
    }

    @Override
    public void disintegrate(
        SessionFactoryImplementor sessionFactory,
        SessionFactoryServiceRegistry serviceRegistry) {

    }
}
