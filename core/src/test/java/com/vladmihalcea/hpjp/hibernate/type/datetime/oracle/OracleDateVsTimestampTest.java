package com.vladmihalcea.hpjp.hibernate.type.datetime.oracle;

import com.vladmihalcea.hpjp.util.AbstractTest;
import com.vladmihalcea.hpjp.util.providers.Database;
import jakarta.persistence.*;
import org.hibernate.cfg.AvailableSettings;
import org.junit.Test;

import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.Properties;

import static com.vladmihalcea.hpjp.hibernate.type.json.model.Participant_.event;
import static org.junit.Assert.assertEquals;

/**
 * @author Vlad Mihalcea
 */
public class OracleDateVsTimestampTest extends AbstractTest {

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
            .setCreatedOn(LocalDateTime.of(2031, 12, 10, 7, 30, 45))
            .setUpdatedOn(LocalDateTime.of(2031, 12, 10, 10, 30, 45, 987654321))
            .setLastAccessedOn(LocalDateTime.of(2031, 12, 10, 12, 30, 45, 987654321));

        doInJPA(entityManager -> {
            entityManager.persist(_event);
        });

        doInJPA(entityManager -> {
            Event event = entityManager.createQuery("""
                select e
                from Event e
                where e.id = :id
                """, Event.class)
                .setParameter("id", 1)
            .getSingleResult();

            assertEquals(
                LocalDateTime.of(2031, 12, 10, 7, 30, 45),
                event.getCreatedOn()
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
    public static class Event {

        @Id
        private Integer id;

        @Column(name = "created_on", columnDefinition = "DATE")
        private LocalDateTime createdOn;

        @Column(name = "updated_on", columnDefinition = "TIMESTAMP(0)")
        private LocalDateTime updatedOn;

        @Column(name = "last_accessed_on", columnDefinition = "TIMESTAMP(6)")
        private LocalDateTime lastAccessedOn;

        public Integer getId() {
            return id;
        }

        public Event setId(Integer id) {
            this.id = id;
            return this;
        }

        public LocalDateTime getCreatedOn() {
            return createdOn;
        }

        public Event setCreatedOn(LocalDateTime dateProperty) {
            this.createdOn = dateProperty;
            return this;
        }

        public LocalDateTime getUpdatedOn() {
            return updatedOn;
        }

        public Event setUpdatedOn(LocalDateTime timestampProperty) {
            this.updatedOn = timestampProperty;
            return this;
        }

        public LocalDateTime getLastAccessedOn() {
            return lastAccessedOn;
        }

        public Event setLastAccessedOn(LocalDateTime timestampPrecisionPreproperty) {
            this.lastAccessedOn = timestampPrecisionPreproperty;
            return this;
        }
    }
}
