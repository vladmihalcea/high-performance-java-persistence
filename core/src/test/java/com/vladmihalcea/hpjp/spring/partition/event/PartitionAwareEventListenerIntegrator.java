package com.vladmihalcea.hpjp.spring.partition.event;

import org.hibernate.boot.Metadata;
import org.hibernate.boot.spi.BootstrapContext;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.event.service.spi.EventListenerRegistry;
import org.hibernate.event.spi.EventType;
import org.hibernate.integrator.spi.Integrator;
import org.hibernate.service.spi.SessionFactoryServiceRegistry;

public class PartitionAwareEventListenerIntegrator implements Integrator {

    public static final PartitionAwareEventListenerIntegrator INSTANCE =
        new PartitionAwareEventListenerIntegrator();

    @Override
    public void integrate(
            Metadata metadata,
            BootstrapContext bootstrapContext,
            SessionFactoryImplementor sessionFactory) {
        sessionFactory
            .getServiceRegistry()
            .getService(EventListenerRegistry.class)
            .prependListeners(
                EventType.PERSIST,
                PartitionAwareInsertEventListener.INSTANCE
            );
    }

    @Override
    public void disintegrate(
        SessionFactoryImplementor sessionFactory,
        SessionFactoryServiceRegistry serviceRegistry) {
    }
}

