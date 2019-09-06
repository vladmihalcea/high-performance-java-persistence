package com.vladmihalcea.book.hpjp.hibernate.query.dto.projection.jpa;

import org.hibernate.boot.Metadata;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.service.spi.SessionFactoryServiceRegistry;

/**
 * @author Vlad Mihalcea
 */
public class DTOImportIntegrator implements org.hibernate.integrator.spi.Integrator {

	public static final DTOImportIntegrator INSTANCE = new DTOImportIntegrator();

	@Override
	public void integrate(
			Metadata metadata,
			SessionFactoryImplementor sessionFactory,
			SessionFactoryServiceRegistry serviceRegistry) {
		metadata.getImports().put("PostDTO", PostDTO.class.getName());
	}

	@Override
	public void disintegrate(
			SessionFactoryImplementor sessionFactory,
			SessionFactoryServiceRegistry serviceRegistry) {

	}
}
