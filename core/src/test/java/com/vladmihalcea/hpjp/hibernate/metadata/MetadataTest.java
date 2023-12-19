package com.vladmihalcea.hpjp.hibernate.metadata;

import com.vladmihalcea.hpjp.util.AbstractMySQLIntegrationTest;
import com.vladmihalcea.hpjp.util.providers.entity.BlogEntityProvider;
import org.hibernate.boot.Metadata;
import org.hibernate.boot.model.relational.Namespace;
import org.hibernate.integrator.spi.Integrator;
import org.hibernate.mapping.Column;
import org.hibernate.mapping.PersistentClass;
import org.hibernate.mapping.Property;
import org.hibernate.mapping.Table;
import org.junit.Test;

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
        for (Namespace namespace : MetadataExtractorIntegrator.INSTANCE.getDatabase().getNamespaces()) {
            for (Table table : namespace.getTables()) {
                LOGGER.info("Table {} has the following columns: {}",
                    table,
                    table.getColumns().stream().map(Column::getName).toList()
                );
            }
        }
    }

    @Test
    public void testEntityToDatabaseBindingMetadata() {
        Metadata metadata = MetadataExtractorIntegrator.INSTANCE.getMetadata();

        for (PersistentClass persistentClass : metadata.getEntityBindings()) {
            Table table = persistentClass.getTable();
            LOGGER.info("Entity: {} is mapped to table: {}",
                persistentClass.getClassName(),
                table.getName()
            );

            for (Property property : persistentClass.getProperties()) {
                for (Column column : property.getColumns()) {
                    LOGGER.info("Property: {} is mapped on table column: {} of type: {}",
                        property.getName(),
                        column.getName(),
                        column.getSqlType()
                    );
                }
            }
        }
    }
}
