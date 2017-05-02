package com.vladmihalcea.book.hpjp.hibernate.metadata;

import java.util.Spliterator;
import java.util.Spliterators;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

import org.hibernate.boot.Metadata;
import org.hibernate.boot.model.relational.Database;
import org.hibernate.boot.model.relational.Namespace;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.integrator.spi.Integrator;
import org.hibernate.mapping.Table;
import org.hibernate.service.spi.SessionFactoryServiceRegistry;

import org.junit.Test;

import com.vladmihalcea.book.hpjp.util.AbstractMySQLIntegrationTest;
import com.vladmihalcea.book.hpjp.util.AbstractTest;
import com.vladmihalcea.book.hpjp.util.providers.entity.BlogEntityProvider;

/**
 * @author Vlad Mihalcea
 */
public class MetadataTest extends AbstractMySQLIntegrationTest {

    public static class MetadataExtractorIntegrator implements org.hibernate.integrator.spi.Integrator {

        public static final MetadataExtractorIntegrator INSTANCE = new MetadataExtractorIntegrator();

        private Database database;

        public Database getDatabase() {
            return database;
        }

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
                LOGGER.info( "Table {} has the following columns: {}",
                     table,
                     StreamSupport.stream(
                         Spliterators.spliteratorUnknownSize( table.getColumnIterator(), Spliterator.ORDERED), false)
                     .collect( Collectors.toList()) );
            }
        }
    }
}
