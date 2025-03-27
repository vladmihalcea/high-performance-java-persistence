package com.vladmihalcea.hpjp.hibernate.type.datetime.oracle;

import com.vladmihalcea.hpjp.util.AbstractTest;
import com.vladmihalcea.hpjp.util.providers.Database;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import org.hibernate.annotations.DynamicUpdate;
import org.junit.Test;

import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;

import static org.junit.Assert.assertEquals;

/**
 * @author Vlad Mihalcea
 */
public class OracleAllTimestampTypesOffsetDateTimeTest extends AbstractTest {

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
        doInJPA(entityManager -> {
            entityManager.createNativeQuery("""
                INSERT INTO event (id, created_on) 
                VALUES (1, TIMESTAMP '2031-12-10 07:30:45.123456')
                """)
            .executeUpdate();
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
                event.getUpdatedOn()
            );
        });
    }

    @Entity(name = "Event")
    @Table(name = "event")
    @DynamicUpdate
    public static class Event {

        @Id
        private Integer id;

        @Column(name = "created_on", columnDefinition = "TIMESTAMP(6)")
        private LocalDateTime createdOn;

        @Column(name = "updated_on", columnDefinition = "TIMESTAMP(9) WITH TIME ZONE")
        private OffsetDateTime updatedOn;

        @Column(name = "last_accessed_on", columnDefinition = "TIMESTAMP WITH LOCAL TIME ZONE")
        private OffsetDateTime lastAccessedOn;

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

        public OffsetDateTime getUpdatedOn() {
            return updatedOn;
        }

        public Event setUpdatedOn(OffsetDateTime timestampProperty) {
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
