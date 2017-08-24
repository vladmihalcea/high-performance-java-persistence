package com.vladmihalcea.book.hpjp.hibernate.metadata;

import java.util.Iterator;
import java.util.Spliterator;
import java.util.Spliterators;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

import org.hibernate.boot.Metadata;
import org.hibernate.boot.model.relational.Namespace;
import org.hibernate.integrator.spi.Integrator;
import org.hibernate.mapping.Column;
import org.hibernate.mapping.PersistentClass;
import org.hibernate.mapping.Property;
import org.hibernate.mapping.Table;

import org.junit.Test;

import com.vladmihalcea.book.hpjp.util.AbstractMySQLIntegrationTest;
import com.vladmihalcea.book.hpjp.util.providers.entity.BlogEntityProvider;

/**
 * @author Vlad Mihalcea
 */
public class MetadataTest extends AbstractMySQLIntegrationTest {

    @Override
    protected Integrator integrator() {
        return MetadataExtractorIntegrator.INSTANCE;
    }

    @Override
    protected Class<?>[] entities() {
        return new BlogEntityProvider().entities();
    }

    @Test
    public void testDatabaseMetadata() {
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
    public void testEntityToDatabaseBindingMetadata() {
        Metadata metadata = MetadataExtractorIntegrator.INSTANCE.getMetadata();

        for ( PersistentClass persistentClass : metadata.getEntityBindings()) {
            Table table = persistentClass.getTable();
            LOGGER.info( "Entity: {} is mapped to table: {}",
                         persistentClass.getClassName(),
                         table.getName()
            );

            for(Iterator propertyIterator =
                    persistentClass.getPropertyIterator(); propertyIterator.hasNext(); ) {
                Property property = (Property) propertyIterator.next();
                for(Iterator columnIterator =
                        property.getColumnIterator(); columnIterator.hasNext(); ) {
                    Column column = (Column) columnIterator.next();
                    LOGGER.info( "Property: {} is mapped on table column: {} of type: {}",
                                 property.getName(),
                                 column.getName(),
                                 column.getSqlType()
                    );
                }
            }
        }
    }
}
