package com.vladmihalcea.hpjp.hibernate.type.datetime;

import com.vladmihalcea.hpjp.util.AbstractTest;
import com.vladmihalcea.hpjp.util.providers.Database;
import jakarta.persistence.*;
import org.hibernate.annotations.DynamicUpdate;
import org.junit.Test;

import java.time.*;

import static org.junit.Assert.assertEquals;

/**
 * @author Vlad Mihalcea
 */
public class TimestampWithTimeZoneVsLocalTest extends AbstractTest {

    @Override
    protected Class<?>[] entities() {
        return new Class<?>[] {
            Event.class
        };
    }

    @Override
    protected Database database() {
        return Database.ORACLE;
    }

    @Test
    public void test() {
        Event _event = new Event()
            .setId(1)
            .setCreatedOn(OffsetDateTime.of(2031, 12, 10, 7, 30, 45, 0, ZoneOffset.of("+09:00")))
            .setUpdatedOn(ZonedDateTime.of(2031, 12, 10, 7, 30, 45, 987654321, ZoneId.of("Asia/Tokyo")))
            .setLastAccessedOn(OffsetDateTime.of(2031, 12, 10, 7, 30, 45, 987654321, ZoneOffset.of("+09:00")));

        doInJPA(entityManager -> {
            entityManager.persist(_event);
        });

        doInJPA(entityManager -> {
            Event event = entityManager.find(Event.class, 1);

            assertEquals(
                OffsetDateTime.of(2031, 12, 10, 7, 30, 45, 0, ZoneOffset.of("+09:00")),
                event.getCreatedOn()
            );
        });

        doInJPA(entityManager -> {
            int updateCount = entityManager.createNativeQuery("""
                UPDATE event
                SET updated_on = TIMESTAMP '2031-12-10 07:30:45.987654321 Europe/Paris'
                WHERE id = 1
                """)
            .executeUpdate();

            assertEquals(1, updateCount);

            Event event = entityManager.find(Event.class, 1);

            assertEquals(
                ZonedDateTime.of(2031, 12, 10, 7, 30, 45, 987654321, ZoneId.of("Europe/Paris")).toOffsetDateTime(),
                event.getUpdatedOn().toOffsetDateTime()
            );
        });

        doInJPA(entityManager -> {
            int updateCount = entityManager.createNativeQuery("""
                UPDATE event
                SET updated_on = TIMESTAMP '2031-12-10 07:30:45.987654321 +02:00'
                WHERE id = 1
                """)
            .executeUpdate();

            assertEquals(1, updateCount);

            Event event = entityManager.find(Event.class, 1);

            assertEquals(
                OffsetDateTime.of(2031, 12, 10, 7, 30, 45, 987654321, ZoneOffset.of("+02:00")),
                event.getUpdatedOn().toOffsetDateTime()
            );
        });

        doInJPA(entityManager -> {
            Tuple bytes = (Tuple) entityManager.createNativeQuery("""
                SELECT 
                    VSIZE(created_on) AS created_on_bytes,
                    VSIZE(updated_on) AS updated_on_bytes,
                    VSIZE(last_accessed_on) AS last_accessed_on_bytes
                FROM event e
                WHERE e.id = :id
                """, Tuple.class)
            .setParameter("id", 1)
            .getSingleResult();

            LOGGER.info("Byte count: {}", bytes);
        });
    }

    @Entity(name = "Event")
    @Table(name = "event")
    @DynamicUpdate
    public static class Event {

        @Id
        private Integer id;

        @Column(name = "created_on", columnDefinition = "TIMESTAMP(0) WITH TIME ZONE")
        private OffsetDateTime createdOn;

        @Column(name = "updated_on", columnDefinition = "TIMESTAMP(9) WITH TIME ZONE")
        private ZonedDateTime updatedOn;

        @Column(name = "last_accessed_on", columnDefinition = "TIMESTAMP WITH LOCAL TIME ZONE")
        private OffsetDateTime lastAccessedOn;

        public Integer getId() {
            return id;
        }

        public Event setId(Integer id) {
            this.id = id;
            return this;
        }

        public OffsetDateTime getCreatedOn() {
            return createdOn;
        }

        public Event setCreatedOn(OffsetDateTime dateProperty) {
            this.createdOn = dateProperty;
            return this;
        }

        public ZonedDateTime getUpdatedOn() {
            return updatedOn;
        }

        public Event setUpdatedOn(ZonedDateTime timestampProperty) {
            this.updatedOn = timestampProperty;
            return this;
        }

        public OffsetDateTime getLastAccessedOn() {
            return lastAccessedOn;
        }

        public Event setLastAccessedOn(OffsetDateTime timestampPrecisionPreproperty) {
            this.lastAccessedOn = timestampPrecisionPreproperty;
            return this;
        }
    }
}
