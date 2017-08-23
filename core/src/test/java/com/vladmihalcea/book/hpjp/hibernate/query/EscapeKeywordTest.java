package com.vladmihalcea.book.hpjp.hibernate.query;

import java.util.List;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Table;

import org.hibernate.Session;

import org.junit.Test;

import com.vladmihalcea.book.hpjp.util.AbstractTest;

import static org.junit.Assert.assertEquals;

/**
 * @author Vlad Mihalcea
 */
public class EscapeKeywordTest extends AbstractTest {

    @Override
    protected Class<?>[] entities() {
        return new Class<?>[] {
            Table.class,
        };
    }

    @Test
    public void test() {
        doInJPA(entityManager -> {
            Table table = new Table();
            table.id = 1L;
            table.from = "from";
            table.number = 123;
            table.select = "*";

            entityManager.persist(table);
        });

        doInJPA(entityManager -> {
            List<Table> tables = entityManager.createQuery(
                "select t " +
                "from Table t " +
                "where t.from like 'from'", Table.class)
            .getResultList();
            assertEquals(1, tables.size());
        });
    }

    @Entity(name = "Table")
    @javax.persistence.Table(name = "\"table\"")
    public static class Table {

        @Id
        private Long id;

        @Column(name = "\"number\"")
        private Integer number;

        @Column(name = "\"from\"")
        private String from;

        @Column(name = "\"select\"")
        private String select;
    }
}
