package com.vladmihalcea.book.hpjp.hibernate.identifier;

import com.vladmihalcea.book.hpjp.util.AbstractTest;
import org.junit.Test;

import javax.persistence.*;
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
        @GeneratedValue(generator = "sequence", strategy=GenerationType.SEQUENCE)
        @SequenceGenerator(name = "sequence", allocationSize = 10)
        private Long id;
    }

    @Entity(name = "tableIdentifier")
    public static class TableSequenceIdentifier {

        @Id
        @GeneratedValue(generator = "table", strategy=GenerationType.TABLE)
        @TableGenerator(name = "table", allocationSize = 10)
        private Long id;
    }
}
