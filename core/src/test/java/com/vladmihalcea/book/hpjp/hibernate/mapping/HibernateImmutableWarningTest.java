package com.vladmihalcea.book.hpjp.hibernate.mapping;

import com.vladmihalcea.book.hpjp.util.AbstractSQLServerIntegrationTest;
import com.vladmihalcea.book.hpjp.util.ReflectionUtils;
import org.hibernate.annotations.Immutable;
import org.hibernate.cfg.AvailableSettings;
import org.junit.Test;

import jakarta.persistence.*;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaUpdate;
import jakarta.persistence.criteria.Root;
import java.util.Date;
import java.util.Properties;

import static org.junit.Assert.assertEquals;

/**
 * @author Vlad Mihalcea
 */
public class HibernateImmutableWarningTest extends AbstractSQLServerIntegrationTest {

    @Override
    protected Class<?>[] entities() {
        return new Class<?>[]{
            Event.class
        };
    }

    @Override
    protected void afterInit() {
        doInJPA(entityManager -> {
            Event event = new Event(
                1L, 
                "Temperature", 
                "25"
            );

            entityManager.persist(event);
        });
    }

    @Test
    public void testFlushChanges() {
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
    }

    @Test
    public void testJPQL() {
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

            assertEquals("10", event.getEventValue());
        });
    }

    @Test
    public void testCriteriaAPI() {

        doInJPA(entityManager -> {
            CriteriaBuilder builder = entityManager.getCriteriaBuilder();
            CriteriaUpdate<Event> update = builder.createCriteriaUpdate(Event.class);

            Root<Event> root = update.from(Event.class);

            update
            .set(root.get("eventValue"), "100")
            .where(
                builder.equal(root.get("id"), 1L)
            );

            entityManager.createQuery(update).executeUpdate();
        });

        doInJPA(entityManager -> {
            Event event = entityManager.find(Event.class, 1L);

            assertEquals("100", event.getEventValue());
        });
    }

    @Entity(name = "Event")
    @Immutable
    public static class Event {

        @Id
        private Long id;

        @Temporal(TemporalType.TIMESTAMP)
        @Column(name = "created_on")
        private Date createdOn = new Date();

        @Column(name = "event_key")
        private String eventKey;

        @Column(name = "event_value")
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
