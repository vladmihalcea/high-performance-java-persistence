package com.vladmihalcea.book.hpjp.hibernate.identifier;

import com.vladmihalcea.book.hpjp.util.AbstractTest;
import org.junit.Test;

import jakarta.persistence.*;
import java.util.Properties;

public class SequenceVsTableGeneratorTest extends AbstractTest {

    @Override
    protected Class<?>[] entities() {
        return new Class<?>[] {
                SequenceIdentifier.class,
                TableSequenceIdentifier.class
        };
    }

    @Override
    protected Properties properties() {
        Properties properties = super.properties();
        return properties;
    }

    @Test
    public void testSequenceIdentifierGenerator() {
        LOGGER.debug("testSequenceIdentifierGenerator");
        doInJPA(entityManager -> {
            for (int i = 0; i < 5; i++) {
                entityManager.persist(new SequenceIdentifier());
            }
            entityManager.flush();
        });
    }

    @Test
    public void testTableSequenceIdentifierGenerator() {
        LOGGER.debug("testTableSequenceIdentifierGenerator");
        doInJPA(entityManager -> {
            for (int i = 0; i < 5; i++) {
                entityManager.persist(new TableSequenceIdentifier());
            }
            entityManager.flush();
        });
    }

    @Entity(name = "sequenceIdentifier")
    public static class SequenceIdentifier {

        @Id
        @GeneratedValue(generator = "hib_sequence", strategy=GenerationType.SEQUENCE)
        @SequenceGenerator(name = "hib_sequence", allocationSize = 10)
        private Long id;
    }

    @Entity(name = "tableIdentifier")
    public static class TableSequenceIdentifier {

        @Id
        @GeneratedValue(generator = "hib_table", strategy=GenerationType.TABLE)
        @TableGenerator(name = "hib_table", allocationSize = 10)
        private Long id;
    }
}
