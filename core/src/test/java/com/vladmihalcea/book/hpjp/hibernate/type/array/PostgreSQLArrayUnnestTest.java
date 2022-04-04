package com.vladmihalcea.book.hpjp.hibernate.type.array;

import com.vladmihalcea.book.hpjp.hibernate.type.json.model.BaseEntity;
import com.vladmihalcea.book.hpjp.util.AbstractPostgreSQLIntegrationTest;
import com.vladmihalcea.book.hpjp.util.providers.DataSourceProvider;
import com.vladmihalcea.book.hpjp.util.providers.PostgreSQLDataSourceProvider;
import org.hibernate.annotations.Type;
import org.hibernate.annotations.TypeDef;
import org.junit.Test;

import javax.persistence.*;

import static org.junit.Assert.assertArrayEquals;

/**
 * @author Vlad Mihalcea
 */
public class PostgreSQLArrayUnnestTest extends AbstractPostgreSQLIntegrationTest {

    @Override
    protected Class<?>[] entities() {
        return new Class<?>[]{
            Event.class,
        };
    }

    @Override
    protected DataSourceProvider dataSourceProvider() {
        return new PostgreSQLDataSourceProvider() {
            @Override
            public String hibernateDialect() {
                return PostgreSQL95ArrayDialect.class.getName();
            }
        };
    }

    @Test
    public void test() {
        doInJPA(entityManager -> {
            entityManager.persist(
                new Event()
                    .setT1(new int[]{1, 3, 6})
                    .setT2(new int[]{8, 9})
            );

            entityManager.persist(
                new Event()
                    .setT1(new int[]{1, 2})
                    .setT2(new int[]{8})
            );

            entityManager.persist(
                new Event()
                    .setT1(new int[]{6})
                    .setT2(new int[]{8, 1})
            );
        });
        //https://stackoverflow.com/questions/23863255/aggregation-to-calculate-number-of-each-tag-where-there-are-two-types-of-tags/23863831#23863831
        doInJPA(entityManager -> {
            Event event = entityManager.find(Event.class, 1L);
        });
    }

    @Entity(name = "Event")
    @Table(name = "event")
    @TypeDef(typeClass = IntArrayType.class, defaultForType = int[].class)
    public static class Event {

        @Id
        @GeneratedValue
        private Long id;

        @Column(columnDefinition = "integer[]")
        private int[] t1;

        @Column(columnDefinition = "integer[]")
        private int[] t2;

        public int[] getT1() {
            return t1;
        }

        public Event setT1(int[] t1) {
            this.t1 = t1;
            return this;
        }

        public int[] getT2() {
            return t2;
        }

        public Event setT2(int[] t2) {
            this.t2 = t2;
            return this;
        }
    }
}
