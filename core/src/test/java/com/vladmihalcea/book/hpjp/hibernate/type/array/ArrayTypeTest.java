package com.vladmihalcea.book.hpjp.hibernate.type.array;

import java.util.List;
import java.util.concurrent.atomic.AtomicReference;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Table;

import org.hibernate.annotations.Type;

import org.junit.Test;

import com.vladmihalcea.book.hpjp.hibernate.type.json.model.BaseEntity;
import com.vladmihalcea.book.hpjp.hibernate.type.json.model.Location;
import com.vladmihalcea.book.hpjp.hibernate.type.json.model.Ticket;
import com.vladmihalcea.book.hpjp.util.AbstractPostgreSQLIntegrationTest;
import com.vladmihalcea.book.hpjp.util.providers.DataSourceProvider;
import com.vladmihalcea.book.hpjp.util.providers.PostgreSQLDataSourceProvider;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;

/**
 * @author Vlad Mihalcea
 */
public class ArrayTypeTest extends AbstractPostgreSQLIntegrationTest {

    @Override
    protected Class<?>[] entities() {
        return new Class<?>[] {
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
            Event nullEvent = new Event();
            nullEvent.setId(0L);
            entityManager.persist(nullEvent);

            Event event = new Event();
            event.setId(1L);
            event.setSensorNames(new String[] {"Temperature", "Pressure"});
            event.setSensorValues( new int[] {12, 756} );
            entityManager.persist(event);
        });
        doInJPA(entityManager -> {
            Event event = entityManager.find(Event.class, 1L);

            assertArrayEquals( new String[] {"Temperature", "Pressure"}, event.getSensorNames() );
            assertArrayEquals( new int[] {12, 756}, event.getSensorValues() );
        });
    }

    @Entity(name = "Event")
    @Table(name = "event")
    public static class Event extends BaseEntity {

        @Type( type = "string-array" )
        @Column(name = "sensor_names", columnDefinition = "text[]")
        private String[] sensorNames;

        @Type( type = "int-array" )
        @Column(name = "sensor_values", columnDefinition = "integer[]")
        private int[] sensorValues;

        public String[] getSensorNames() {
            return sensorNames;
        }

        public void setSensorNames(String[] sensorNames) {
            this.sensorNames = sensorNames;
        }

        public int[] getSensorValues() {
            return sensorValues;
        }

        public void setSensorValues(int[] sensorValues) {
            this.sensorValues = sensorValues;
        }
    }

}
