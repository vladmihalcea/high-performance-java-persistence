package com.vladmihalcea.hpjp.hibernate.metadata;

import org.hibernate.boot.Metadata;
import org.hibernate.boot.model.relational.Database;
import org.hibernate.boot.spi.BootstrapContext;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.event.service.spi.EventListenerRegistry;
import org.hibernate.service.spi.SessionFactoryServiceRegistry;

/**
 * @author Vlad Mihalcea
 */
public class MetadataExtractorIntegrator implements org.hibernate.integrator.spi.Integrator {

	public static final MetadataExtractorIntegrator INSTANCE = new MetadataExtractorIntegrator();

	private Database database;

	private Metadata metadata;

	public Database getDatabase() {
		return database;
	}

	public Metadata getMetadata() {
		return metadata;
	}

	@Override
	public void integrate(Metadata metadata, BootstrapContext bootstrapContext, SessionFactoryImplementor sessionFactory) {
		final EventListenerRegistry eventListenerRegistry = sessionFactory.getServiceRegistry().getService(EventListenerRegistry.class);

		this.database = metadata.getDatabase();
		this.metadata = metadata;
	}

	@Override
	public void disintegrate(
			SessionFactoryImplementor sessionFactory,
			SessionFactoryServiceRegistry serviceRegistry) {

	}
}
