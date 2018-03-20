package com.vladmihalcea.book.hpjp.hibernate.mapping;

import com.vladmihalcea.book.hpjp.util.AbstractSQLServerIntegrationTest;
import com.vladmihalcea.book.hpjp.util.ReflectionUtils;
import org.hibernate.annotations.Immutable;
import org.junit.Test;

import javax.persistence.*;
import java.util.Date;

import static org.junit.Assert.assertEquals;

/**
 * @author Vlad Mihalcea
 */
public class JPAImmutableTest extends AbstractSQLServerIntegrationTest {

    @Override
    protected Class<?>[] entities() {
        return new Class<?>[]{
                Event.class
        };
    }

    @Test
    public void test() {
        doInJPA(entityManager -> {
            Event event = new Event(1L, "Temperature", "25");

            entityManager.persist(event);
        });

        doInJPA(entityManager -> {
            Event event = entityManager.find(Event.class, 1L);

            assertEquals("25", event.getEventValue());

            ReflectionUtils.setFieldValue(event, "eventValue", "10");
            assertEquals("10", event.getEventValue());
        });

        doInJPA(entityManager -> {
            Event event = entityManager.find(Event.class, 1L);

            assertEquals("25", event.getEventValue());
        });

        doInJPA(entityManager -> {
            entityManager.createQuery(
                "update Event " +
                "set eventValue = :eventValue " +
                "where id = :id")
            .setParameter("eventValue", "10")
            .setParameter("id", 1L)
            .executeUpdate();
        });

        doInJPA(entityManager -> {
            Event event = entityManager.find(Event.class, 1L);

            assertEquals("25", event.getEventValue());
        });
    }

    @Entity(name = "Event")
    public static class Event {

        @Id
        private Long id;

        @Temporal(TemporalType.TIMESTAMP)
        @Column(name = "created_on", updatable = false)
        private Date createdOn = new Date();

        @Column(name = "event_key", updatable = false)
        private String eventKey;

        @Column(name = "event_value", updatable = false)
        private String eventValue;

        public Event(Long id, String eventKey, String eventValue) {
            this.id = id;
            this.eventKey = eventKey;
            this.eventValue = eventValue;
        }

        private Event() {
        }

        public Long getId() {
            return id;
        }

        public Date getCreatedOn() {
            return createdOn;
        }

        public String getEventKey() {
            return eventKey;
        }

        public String getEventValue() {
            return eventValue;
        }
    }
}
