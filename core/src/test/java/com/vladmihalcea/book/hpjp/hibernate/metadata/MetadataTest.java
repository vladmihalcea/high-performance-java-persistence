package com.vladmihalcea.book.hpjp.hibernate.metadata;

import java.util.Collection;
import javax.persistence.Entity;
import javax.persistence.Id;

import org.hibernate.annotations.DynamicUpdate;
import org.hibernate.boot.Metadata;
import org.hibernate.boot.model.relational.Database;
import org.hibernate.boot.model.relational.Namespace;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.integrator.spi.Integrator;
import org.hibernate.mapping.Table;
import org.hibernate.service.spi.SessionFactoryServiceRegistry;

import org.junit.Test;

import com.vladmihalcea.book.hpjp.util.AbstractTest;
import com.vladmihalcea.book.hpjp.util.EntityProvider;
import com.vladmihalcea.book.hpjp.util.providers.entity.BlogEntityProvider;

/**
 * @author Vlad Mihalcea
 */
public class MetadataTest extends AbstractTest {

    public static class MetadataExtractorIntegrator implements org.hibernate.integrator.spi.Integrator {

        public static final MetadataExtractorIntegrator INSTANCE = new MetadataExtractorIntegrator();

        private Database database;

        @Override
        public void integrate(
                Metadata metadata,
                SessionFactoryImplementor sessionFactory,
                SessionFactoryServiceRegistry serviceRegistry) {

            database = metadata.getDatabase();
        }

        @Override
        public void disintegrate(
            SessionFactoryImplementor sessionFactory,
            SessionFactoryServiceRegistry serviceRegistry) {

        }

        public Database getDatabase() {
            return database;
        }
    }

    @Override
    protected Integrator integrator() {
        return MetadataExtractorIntegrator.INSTANCE;
    }

    @Override
    protected Class<?>[] entities() {
        return new BlogEntityProvider().entities();
    }

    @Test
    public void test() {
        for(Namespace namespace : MetadataExtractorIntegrator.INSTANCE.getDatabase().getNamespaces()) {
            for( Table table : namespace.getTables()) {
                LOGGER.info( "Mapping table: {}", table );
            }
        }
    }
}
