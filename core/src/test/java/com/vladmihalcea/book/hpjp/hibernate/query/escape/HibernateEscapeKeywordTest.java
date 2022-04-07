package com.vladmihalcea.book.hpjp.hibernate.query.escape;

import com.vladmihalcea.book.hpjp.util.AbstractTest;
import org.junit.Test;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import java.util.List;

import static org.junit.Assert.assertEquals;

/**
 * @author Vlad Mihalcea
 */
public class HibernateEscapeKeywordTest extends AbstractTest {

    @Override
    protected Class<?>[] entities() {
        return new Class<?>[] {
            Table.class,
        };
    }

    @Test
    public void test() {
        doInJPA(entityManager -> {
            entityManager.persist(
                new Table()
                .setCatalog("library")
                .setSchema("public")
                .setName("book")
                .setDescription("The book table stores book-related info")
            );
        });

        doInJPA(entityManager -> {
            List<Table> tables = entityManager.createQuery(
                "select t " +
                "from Table t " +
                "where t.description like '%book%'", Table.class)
            .getResultList();

            assertEquals(1, tables.size());
        });
    }

    @Entity(name = "Table")
    @jakarta.persistence.Table(name = "`table`")
    public static class Table {

        @Id
        @GeneratedValue
        private Long id;

        @Column(name = "`catalog`")
        private String catalog;

        @Column(name = "`schema`")
        private String schema;

        @Column(name = "`name`")
        private String name;

        @Column(name = "`desc`")
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
