package com.vladmihalcea.book.hpjp.hibernate.query.escape;

import com.vladmihalcea.book.hpjp.util.AbstractTest;
import org.hibernate.cfg.AvailableSettings;
import org.junit.Test;

import jakarta.persistence.Entity;
import jakarta.persistence.EntityManagerFactory;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import java.util.List;
import java.util.Properties;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

/**
 * @author Vlad Mihalcea
 */
public class NoEscapeKeywordTest extends AbstractTest {

    @Override
    protected Class<?>[] entities() {
        return new Class<?>[] {
            Table.class,
        };
    }

    @Override
    protected void additionalProperties(Properties properties) {
        properties.put(AvailableSettings.HBM2DDL_HALT_ON_ERROR, Boolean.TRUE);
    }

    @Override
    public EntityManagerFactory newEntityManagerFactory() {
        EntityManagerFactory entityManagerFactory = null;
        try {
            entityManagerFactory = super.newEntityManagerFactory();
            fail("Should have thrown exception!");
        } catch (Exception expected) {
            expected.getMessage();
        }
        return entityManagerFactory;
    }

    @Test
    public void test() {

    }

    @Entity(name = "Table")
    public static class Table {

        @Id
        @GeneratedValue
        private Long id;

        private String catalog;

        private String schema;

        private String name;

        private String description;

        public Long getId() {
            return id;
        }

        public Table setId(Long id) {
            this.id = id;
            return this;
        }

        public String getCatalog() {
            return catalog;
        }

        public Table setCatalog(String catalog) {
            this.catalog = catalog;
            return this;
        }

        public String getSchema() {
            return schema;
        }

        public Table setSchema(String schema) {
            this.schema = schema;
            return this;
        }

        public String getName() {
            return name;
        }

        public Table setName(String name) {
            this.name = name;
            return this;
        }

        public String getDescription() {
            return description;
        }

        public Table setDescription(String description) {
            this.description = description;
            return this;
        }
    }
}
