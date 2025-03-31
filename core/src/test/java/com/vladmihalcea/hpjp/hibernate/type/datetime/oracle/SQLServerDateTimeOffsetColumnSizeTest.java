package com.vladmihalcea.hpjp.hibernate.type.datetime.oracle;

import com.vladmihalcea.hpjp.util.AbstractTest;
import com.vladmihalcea.hpjp.util.providers.Database;
import jakarta.persistence.*;
import org.hibernate.annotations.DynamicUpdate;
import org.junit.Test;

import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;

/**
 * @author Vlad Mihalcea
 */
public class SQLServerDateTimeOffsetColumnSizeTest extends AbstractTest {

    @Override
    protected Class<?>[] entities() {
        return new Class<?>[] {
            Event.class
        };
    }

    @Override
    protected Database database() {
        return Database.SQLSERVER;
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
            Tuple bytes = (Tuple) entityManager.createNativeQuery("""
                SELECT
                    DATALENGTH(created_on) AS created_on_bytes,
                    DATALENGTH(updated_on) AS updated_on_bytes,
                    DATALENGTH(last_accessed_on) AS last_accessed_on_bytes
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

        @Column(name = "created_on", columnDefinition = "datetimeoffset(0)")
        private OffsetDateTime createdOn;

        @Column(name = "updated_on", columnDefinition = "datetimeoffset(3)")
        private ZonedDateTime updatedOn;

        @Column(name = "last_accessed_on", columnDefinition = "datetimeoffset(5)")
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
