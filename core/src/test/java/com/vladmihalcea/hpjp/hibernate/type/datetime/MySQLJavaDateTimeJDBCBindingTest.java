package com.vladmihalcea.hpjp.hibernate.type.datetime;

import com.vladmihalcea.hpjp.util.AbstractTest;
import com.vladmihalcea.hpjp.util.providers.Database;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import org.hibernate.cfg.AvailableSettings;
import org.junit.Test;

import java.time.LocalDateTime;
import java.util.Properties;

import static org.junit.Assert.assertEquals;

/**
 * @author Vlad Mihalcea
 */
public class MySQLJavaDateTimeJDBCBindingTest extends AbstractTest {

    @Override
    protected Class<?>[] entities() {
        return new Class<?>[] {
            DateTimeEvent.class
        };
    }

    @Override
    protected Database database() {
        return Database.MYSQL;
    }

    @Override
    protected void additionalProperties(Properties properties) {
        properties.setProperty(AvailableSettings.JAVA_TIME_USE_DIRECT_JDBC, "true");
    }

    @Test
    public void test() {
        DateTimeEvent _event = new DateTimeEvent()
            .setId(1L)
            .setLocalDateTime(LocalDateTime.of(2024, 6, 18, 12, 34));

        doInJPA(entityManager -> {
            entityManager.persist(_event);
        });

        doInJPA(entityManager -> {
            DateTimeEvent event = entityManager.createQuery("""
                select e
                from DateTimeEvent e
                where e.localDateTime = :localDateTime
                """, DateTimeEvent.class)
                .setParameter("localDateTime", _event.getLocalDateTime())
                .getSingleResult();

            assertEquals(
                LocalDateTime.of(2024, 6, 18, 12, 34),
                event.getLocalDateTime()
            );
        });
    }

    @Entity(name = "DateTimeEvent")
    @Table(name = "date_time_event")
    public static class DateTimeEvent {

        @Id
        private Long id;

        @Column(name = "local_date_time")
        private LocalDateTime localDateTime;

        public Long getId() {
            return id;
        }

        public DateTimeEvent setId(Long id) {
            this.id = id;
            return this;
        }

        public LocalDateTime getLocalDateTime() {
            return localDateTime;
        }

        public DateTimeEvent setLocalDateTime(LocalDateTime localDateTime) {
            this.localDateTime = localDateTime;
            return this;
        }
    }
}
