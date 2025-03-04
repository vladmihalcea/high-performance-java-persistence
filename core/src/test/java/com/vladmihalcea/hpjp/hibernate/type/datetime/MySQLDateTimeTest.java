package com.vladmihalcea.hpjp.hibernate.type.datetime;

import com.vladmihalcea.hpjp.util.AbstractTest;
import com.vladmihalcea.hpjp.util.providers.Database;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import org.junit.Test;

import java.time.LocalDateTime;

/**
 * @author Vlad Mihalcea
 */
public class MySQLDateTimeTest extends AbstractTest {

    @Override
    protected Class<?>[] entities() {
        return new Class<?>[] {
            DateTimeEvent.class,
            TimestampEvent.class
        };
    }

    @Override
    protected Database database() {
        return Database.MYSQL;
    }

    @Test
    public void testLocalDateEvent() {
        doInJPA(entityManager -> {
            DateTimeEvent dateTimeEvent = new DateTimeEvent();
            dateTimeEvent.id = LocalDateTime.now();
            entityManager.persist(dateTimeEvent);

            TimestampEvent timestampEvent = new TimestampEvent();
            timestampEvent.id = LocalDateTime.now();
            entityManager.persist(timestampEvent);
        });
        LOGGER.info("Data created");
    }

    @Entity(name = "DateTimeEvent")
    @Table(name = "date_time_event")
    public static class DateTimeEvent {

        @Id
        @Column(columnDefinition = "DATETIME")
        private LocalDateTime id;
    }

    @Entity(name = "TimestampEvent")
    @Table(name = "timestamp_event")
    public static class TimestampEvent {

        @Id
        @Column(columnDefinition = "DATETIME")
        private LocalDateTime id;
    }
}
