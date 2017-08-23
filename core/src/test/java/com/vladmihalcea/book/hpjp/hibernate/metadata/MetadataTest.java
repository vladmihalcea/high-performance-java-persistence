package com.vladmihalcea.book.hpjp.hibernate.metadata;

import java.util.Iterator;
import java.util.Spliterator;
import java.util.Spliterators;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

import org.hibernate.boot.Metadata;
import org.hibernate.boot.model.relational.Database;
import org.hibernate.boot.model.relational.Namespace;
import org.hibernate.cfg.annotations.EntityBinder;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.integrator.spi.Integrator;
import org.hibernate.mapping.Column;
import org.hibernate.mapping.PersistentClass;
import org.hibernate.mapping.Property;
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

        private Metadata metadata;

        public Database getDatabase() {
            return database;
        }

        public Metadata getMetadata() {
            return metadata;
        }

        @Override
        public void integrate(
                Metadata metadata,
                SessionFactoryImplementor sessionFactory,
                SessionFactoryServiceRegistry serviceRegistry) {

            this.database = metadata.getDatabase();
            this.metadata = metadata;

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

    @Test
    public void testMetadata() {
        Metadata metadata = MetadataExtractorIntegrator.INSTANCE.getMetadata();

        PersistentClass persistentClass = metadata.getEntityBinding( BlogEntityProvider.Post.class.getName());
        Table table = persistentClass.getTable();

        for(Iterator propertyIterator =
                persistentClass.getPropertyIterator(); propertyIterator.hasNext(); ) {
           Property property = (Property) propertyIterator.next();
            for(Iterator columnIterator =
                    property.getColumnIterator(); columnIterator.hasNext(); ) {
                Column column = (Column) columnIterator.next();
                LOGGER.info( "Table {}, column {} associated to property: {}",
                         table.getName(),
                         column.getName(),
                         property.getName()
                );
            }
        }
    }
}
