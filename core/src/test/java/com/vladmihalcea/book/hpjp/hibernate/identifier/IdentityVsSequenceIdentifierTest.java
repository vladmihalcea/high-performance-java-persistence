package com.vladmihalcea.book.hpjp.hibernate.identifier;

import com.vladmihalcea.book.hpjp.util.AbstractTest;
import org.hibernate.annotations.GenericGenerator;
import org.junit.Test;

import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import java.util.Properties;

public class IdentityVsSequenceIdentifierTest extends AbstractTest {

    @Override
    protected Class<?>[] entities() {
        return new Class<?>[] {
                IdentityIdentifier.class,
                SequenceIdentifier.class,
                TableSequenceIdentifier.class,
                AssignTableSequenceIdentifier.class
        };
    }

    @Override
    protected Properties properties() {
        Properties properties = super.properties();
        properties.put("hibernate.order_inserts", "true");
        properties.put("hibernate.order_updates", "true");
        properties.put("hibernate.jdbc.batch_size", "2");
        return properties;
    }

    @Test
    public void testIdentityIdentifierGenerator() {
        LOGGER.debug("testIdentityIdentifierGenerator");
        doInJPA(entityManager -> {
                for (int i = 0; i < 5; i++) {
                    entityManager.persist(new IdentityIdentifier());
                }
                entityManager.flush();
                return null;

        });
    }

    @Test
    public void testSequenceIdentifierGenerator() {
        LOGGER.debug("testSequenceIdentifierGenerator");
        doInJPA(entityManager -> {
                for (int i = 0; i < 5; i++) {
                    entityManager.persist(new SequenceIdentifier());
                }
                entityManager.flush();
                return null;

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
                return null;

        });
    }

    @Test
    public void testAssignTableSequenceIdentifierGenerator() {
        LOGGER.debug("testAssignTableSequenceIdentifierGenerator");
        doInJPA(entityManager -> {
            for (int i = 0; i < 5; i++) {
                entityManager.persist(new AssignTableSequenceIdentifier());
            }
            AssignTableSequenceIdentifier tableSequenceIdentifier = new AssignTableSequenceIdentifier();
            tableSequenceIdentifier.id = -1L;
            entityManager.merge(tableSequenceIdentifier);
            entityManager.flush();
        });
    }

    @Entity(name = "identityIdentifier")
    public static class IdentityIdentifier {

        @Id
        @GeneratedValue(strategy = GenerationType.IDENTITY)
        private Long id;
    }

    @Entity(name = "sequenceIdentifier")
    public static class SequenceIdentifier {

        @Id
        @GenericGenerator(name = "sequence", strategy = "sequence", parameters = {
                @org.hibernate.annotations.Parameter(name = "sequenceName", value = "sequence"),
                @org.hibernate.annotations.Parameter(name = "allocationSize", value = "1"),
        })
        @GeneratedValue(generator = "sequence", strategy=GenerationType.SEQUENCE)
        private Long id;
    }

    @Entity(name = "tableIdentifier")
    public static class TableSequenceIdentifier {

        @Id
        @GenericGenerator(name = "table", strategy = "enhanced-table", parameters = {
                @org.hibernate.annotations.Parameter(name = "table_name", value = "sequence_table")
        })
        @GeneratedValue(generator = "table", strategy=GenerationType.TABLE)
        private Long id;
    }

    @Entity(name = "assigneTableIdentifier")
    public static class AssignTableSequenceIdentifier implements Identifiable<Long> {

        @Id
        @GenericGenerator(name = "table", strategy = "com.vladmihalcea.hibernate.masterclass.laboratory.idgenerator.AssignedTableGenerator",
            parameters = {
                @org.hibernate.annotations.Parameter(name = "table_name", value = "sequence_table")
        })
        @GeneratedValue(generator = "table", strategy=GenerationType.TABLE)
        private Long id;

        @Override
        public Long getId() {
            return id;
        }
    }

}
