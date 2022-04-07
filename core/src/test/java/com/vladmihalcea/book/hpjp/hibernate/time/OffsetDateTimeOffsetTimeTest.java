package com.vladmihalcea.book.hpjp.hibernate.time;

import com.vladmihalcea.book.hpjp.util.AbstractMySQLIntegrationTest;
import org.hibernate.cfg.AvailableSettings;
import org.junit.Ignore;
import org.junit.Test;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.sql.Time;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.OffsetTime;
import java.time.ZoneOffset;
import java.util.Properties;

import static org.junit.Assert.assertEquals;

/**
 * @author Vlad Mihalcea
 */
public class OffsetDateTimeOffsetTimeTest extends AbstractMySQLIntegrationTest {

    @Override
    protected Class<?>[] entities() {
        return new Class<?>[]{
            Notification.class
        };
    }

    @Override
    protected void additionalProperties(Properties properties) {
        properties.setProperty(AvailableSettings.JDBC_TIME_ZONE, "UTC");
    }

    @Test
    @Ignore
    public void test() {
        ZoneOffset offset = OffsetDateTime.now().getOffset();
        OffsetTime offsetTime = OffsetTime.of(7, 30, 0, 0, offset);

        doInJPA(entityManager -> {
            Notification notification = new Notification()
                .setId(1L)
                .setCreatedOn(
                    LocalDateTime.of(
                        2020, 5, 1,
                        12, 30, 0
                    ).atOffset(offset)
                ).setClockAlarm(offsetTime);

            entityManager.persist(notification);
        });

        doInJPA(entityManager -> {
            Notification notification = entityManager.find(
                Notification.class, 1L
            );

            assertEquals(
                LocalDateTime.of(
                    2020, 5, 1,
                    12, 30, 0
                ).atOffset(offset),
                notification.getCreatedOn()
            );

            assertEquals(
                java.sql.Time.valueOf(offsetTime.toLocalTime()).toLocalTime().atOffset(offset),
                notification.getClockAlarm()
            );
        });
    }

    @Entity(name = "Notification")
    @Table(name = "notification")
    public static class Notification {

        @Id
        private Long id;

        @Column(name = "created_on")
        private OffsetDateTime createdOn;

        @Column(name = "notify_on")
        private OffsetTime clockAlarm;

        public Long getId() {
            return id;
        }

        public Notification setId(Long id) {
            this.id = id;
            return this;
        }

        public OffsetDateTime getCreatedOn() {
            return createdOn;
        }

        public Notification setCreatedOn(OffsetDateTime createdOn) {
            this.createdOn = createdOn;
            return this;
        }

        public OffsetTime getClockAlarm() {
            return clockAlarm;
        }

        public Notification setClockAlarm(OffsetTime clockAlarm) {
            this.clockAlarm = clockAlarm;
            return this;
        }
    }
}
