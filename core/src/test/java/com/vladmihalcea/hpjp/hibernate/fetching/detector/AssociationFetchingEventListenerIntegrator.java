package com.vladmihalcea.hpjp.hibernate.fetching.detector;

import org.hibernate.boot.Metadata;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.event.service.spi.EventListenerRegistry;
import org.hibernate.event.spi.EventType;
import org.hibernate.integrator.spi.Integrator;
import org.hibernate.service.spi.SessionFactoryServiceRegistry;

/**
 * @author Vlad Mihalcea
 */
public class AssociationFetchingEventListenerIntegrator implements Integrator {

    public static final AssociationFetchingEventListenerIntegrator INSTANCE = new AssociationFetchingEventListenerIntegrator();

    @Override
    public void integrate(
        Metadata metadata,
        SessionFactoryImplementor sessionFactory,
        SessionFactoryServiceRegistry serviceRegistry) {

        final EventListenerRegistry eventListenerRegistry =
            serviceRegistry.getService(EventListenerRegistry.class);

        eventListenerRegistry.prependListeners(
            EventType.LOAD,
            AssociationFetchPreLoadEventListener.INSTANCE
        );

        eventListenerRegistry.appendListeners(
            EventType.LOAD,
            AssociationFetchLoadEventListener.INSTANCE
        );

        eventListenerRegistry.appendListeners(
            EventType.POST_LOAD,
            AssociationFetchPostLoadEventListener.INSTANCE
        );
    }

    @Override
    public void disintegrate(
        SessionFactoryImplementor sessionFactory,
        SessionFactoryServiceRegistry serviceRegistry) {

    }
}
