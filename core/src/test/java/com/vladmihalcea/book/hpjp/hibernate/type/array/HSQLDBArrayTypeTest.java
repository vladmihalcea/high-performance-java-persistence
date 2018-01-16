package com.vladmihalcea.book.hpjp.hibernate.type.array;

import com.vladmihalcea.book.hpjp.util.AbstractTest;
import com.vladmihalcea.book.hpjp.util.providers.DataSourceProvider;
import com.vladmihalcea.book.hpjp.util.providers.HSQLDBDataSourceProvider;
import org.hibernate.annotations.Type;
import org.hibernate.annotations.TypeDef;
import org.hibernate.annotations.TypeDefs;
import org.hibernate.dialect.HSQLDialect;
import org.junit.Test;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Table;

import java.sql.Types;

import static org.junit.Assert.assertArrayEquals;

/**
 * @author Vlad Mihalcea
 */
public class HSQLDBArrayTypeTest extends AbstractTest {

    @Override
    protected Class<?>[] entities() {
        return new Class<?>[]{
            Event.class,
        };
    }

    /*public static class HSQLDialectArrayDialect extends HSQLDialect {

        public HSQLDialectArrayDialect() {
            super();
            this.registerHibernateType(Types.ARRAY, "string-array");
        }
    }

    @Override
    protected DataSourceProvider dataSourceProvider() {
        return new HSQLDBDataSourceProvider() {
            @Override
            public String hibernateDialect() {
                return HSQLDialectArrayDialect.class.getName();
            }
        };
    }*/

    @Test
    public void test() {
        doInJPA(entityManager -> {
            Event nullEvent = new Event();
            nullEvent.setId(0L);
            entityManager.persist(nullEvent);

            Event event = new Event();
            event.setId(1L);
            event.setSensorNames(new String[]{"Temperature", "Pressure"});
            event.setSensorValues(new Integer[]{12, 756});
            entityManager.persist(event);
        });

        doInJPA(entityManager -> {
            Event event = entityManager.find(Event.class, 1L);

            assertArrayEquals(new String[]{"Temperature", "Pressure"}, event.getSensorNames());
            assertArrayEquals(new Integer[]{12, 756}, event.getSensorValues());
        });

        doInJPA(entityManager -> {
            Event event = entityManager.createQuery(
                "select e " +
                "from Event e " +
                "where e.sensorNames = :sensorNames", Event.class)
            .setParameter("sensorNames", new String[]{"Temperature", "Pressure"})
            .getSingleResult();

            assertArrayEquals(new String[]{"Temperature", "Pressure"}, event.getSensorNames());
            assertArrayEquals(new Integer[]{12, 756}, event.getSensorValues());
        });

        /*doInJPA(entityManager -> {
            Integer[] sensorValues = (Integer[]) entityManager.createNativeQuery(
                "select e.sensor_values " +
                "from Event e " +
                "where e.sensor_names = :sensorNames")
            .unwrap(org.hibernate.query.NativeQuery.class)
            .setParameter("sensorNames", new String[]{"Temperature", "Pressure"}, VarCharStringArrayType.INSTANCE)
            .getSingleResult();

            assertArrayEquals(new Integer[]{12, 756}, sensorValues);
        });*/
    }

    @Entity(name = "Event")
    @Table(name = "event")
    @TypeDefs({
        @TypeDef(name = "string-array", typeClass = VarCharStringArrayType.class),
        @TypeDef(name = "int-array", typeClass = IntArrayType.class),
    })
    public static class Event {

        @Id
        private Long id;

        @Type(type = "string-array")
        @Column(name = "sensor_names", columnDefinition = "VARCHAR(100) ARRAY")
        private String[] sensorNames;

        @Type(type = "int-array")
        @Column(name = "sensor_values", columnDefinition = "INT ARRAY")
        private Integer[] sensorValues;

        public Long getId() {
            return id;
        }

        public void setId(Long id) {
            this.id = id;
        }

        public String[] getSensorNames() {
            return sensorNames;
        }

        public void setSensorNames(String[] sensorNames) {
            this.sensorNames = sensorNames;
        }

        public Integer[] getSensorValues() {
            return sensorValues;
        }

        public void setSensorValues(Integer[] sensorValues) {
            this.sensorValues = sensorValues;
        }
    }

}
