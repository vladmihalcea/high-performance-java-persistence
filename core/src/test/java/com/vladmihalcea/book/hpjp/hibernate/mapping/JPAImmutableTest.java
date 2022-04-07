package com.vladmihalcea.book.hpjp.hibernate.mapping;

import com.vladmihalcea.book.hpjp.util.AbstractSQLServerIntegrationTest;
import com.vladmihalcea.book.hpjp.util.ReflectionUtils;
import com.vladmihalcea.book.hpjp.util.exception.ExceptionUtil;
import org.hibernate.annotations.Immutable;
import org.hibernate.cfg.AvailableSettings;
import org.hibernate.query.ImmutableEntityUpdateQueryHandlingMode;
import org.junit.Test;

import jakarta.persistence.*;
import java.util.Date;
import java.util.Properties;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

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

    @Override
    protected void additionalProperties(Properties properties) {
        properties.put(AvailableSettings.IMMUTABLE_ENTITY_UPDATE_QUERY_HANDLING_MODE, ImmutableEntityUpdateQueryHandlingMode.EXCEPTION);
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

        try {
            doInJPA(entityManager -> {
                entityManager.createQuery(
                    "update Event " +
                    "set eventValue = :eventValue " +
                    "where id = :id")
                .setParameter("eventValue", "10")
                .setParameter("id", 1L)
                .executeUpdate();
            });

            fail("Should have thrown Exception");
        } catch (Exception expected) {
            assertEquals("The query: [update Event set eventValue = :eventValue where id = :id] attempts to update an immutable entity: [Event]", ExceptionUtil.rootCause(expected).getMessage());
        }

        doInJPA(entityManager -> {
            Event event = entityManager.find(Event.class, 1L);

            assertEquals("25", event.getEventValue());
        });
    }

    @Entity(name = "Event")
    @Immutable
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
