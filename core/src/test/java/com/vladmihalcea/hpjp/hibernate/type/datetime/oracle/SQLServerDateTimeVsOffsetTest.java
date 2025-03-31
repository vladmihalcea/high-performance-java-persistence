package com.vladmihalcea.hpjp.hibernate.type.datetime.oracle;

import com.vladmihalcea.hpjp.util.AbstractTest;
import com.vladmihalcea.hpjp.util.providers.Database;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import org.hibernate.Session;
import org.hibernate.annotations.DynamicUpdate;
import org.hibernate.annotations.JdbcType;
import org.hibernate.type.descriptor.jdbc.ZonedDateTimeJdbcType;
import org.junit.Test;

import java.sql.ResultSet;
import java.sql.Statement;
import java.time.*;

import static org.junit.Assert.assertEquals;

/**
 * @author Vlad Mihalcea
 */
public class SQLServerDateTimeVsOffsetTest extends AbstractTest {

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
        doInJPA(entityManager -> {
            entityManager.createNativeQuery("""
                INSERT INTO event (id, created_on) 
                VALUES (1, '2031-12-10 07:30:45.1234567 +12:00')
                """)
            .executeUpdate();
        });

        doInJPA(entityManager -> {
            entityManager.unwrap(Session.class).doWork(connection -> {
                try(Statement statement = connection.createStatement()) {
                    ResultSet resultSet = statement.executeQuery("""
                        SELECT created_on 
                        FROM event
                        WHERE id = 1
                        """);

                    LOGGER.info("{}{}", System.lineSeparator(), resultSetToString(resultSet));
                }
            });

            Event event = entityManager.find(Event.class, 1);

            assertEquals(
                LocalDateTime.of(2031, 12, 10, 7, 30, 45, 123456700),
                event.getCreatedOn()
            );
        });

        doInJPA(entityManager -> {
            int updateCount = entityManager.createNativeQuery("""
                UPDATE event
                SET updated_on = '2031-12-10 07:30:45.9876543 +01:00'
                WHERE id = 1
                """)
            .executeUpdate();

            assertEquals(1, updateCount);
        });

        doInJPA(entityManager -> {
            entityManager.unwrap(Session.class).doWork(connection -> {
                try(Statement statement = connection.createStatement()) {
                    ResultSet resultSet = statement.executeQuery("""
                        SELECT updated_on 
                        FROM event
                        WHERE id = 1
                        """);

                    LOGGER.info("{}{}", System.lineSeparator(), resultSetToString(resultSet));
                }
            });

            Event event = entityManager.find(Event.class, 1);

            assertEquals(
                OffsetDateTime.of(2031, 12, 10, 7, 30, 45, 987654300, ZoneOffset.of("+01:00")),
                event.getUpdatedOn()
            );

            event.setUpdatedOn(
                OffsetDateTime.of(2031, 12, 10, 7, 30, 45, 987654300, ZoneOffset.of("+09:00"))
            );
        });

        doInJPA(entityManager -> {
            entityManager.unwrap(Session.class).doWork(connection -> {
                try(Statement statement = connection.createStatement()) {
                    ResultSet resultSet = statement.executeQuery("""
                        SELECT updated_on 
                        FROM event
                        WHERE id = 1
                        """);

                    LOGGER.info("{}{}", System.lineSeparator(), resultSetToString(resultSet));
                }
            });
        });

        doInJPA(entityManager -> {
            int updateCount = entityManager.createNativeQuery("""
                UPDATE event
                SET updated_on = '2031-12-10 07:30:45.9876543 +00:00'
                WHERE id = 1
                """)
            .executeUpdate();

            assertEquals(1, updateCount);

            entityManager.unwrap(Session.class).doWork(connection -> {
                try(Statement statement = connection.createStatement()) {
                    ResultSet resultSet = statement.executeQuery("""
                        SELECT updated_on 
                        FROM event
                        WHERE id = 1
                        """);

                    LOGGER.info("{}{}", System.lineSeparator(), resultSetToString(resultSet));
                }
            });
        });

        doInJPA(entityManager -> {
            int updateCount = entityManager.createNativeQuery("""
                UPDATE event
                SET last_accessed_on = '2031-12-10 07:30:45'
                WHERE id = 1
                """)
                .executeUpdate();

            assertEquals(1, updateCount);
        });

        doInJPA(entityManager -> {
            entityManager.unwrap(Session.class).doWork(connection -> {
                try(Statement statement = connection.createStatement()) {
                    ResultSet resultSet = statement.executeQuery("""
                        SELECT last_accessed_on 
                        FROM event
                        WHERE id = 1
                        """);

                    LOGGER.info("{}{}", System.lineSeparator(), resultSetToString(resultSet));
                }
            });
        });
    }

    @Entity(name = "Event")
    @Table(name = "event")
    @DynamicUpdate
    public static class Event {

        @Id
        private Integer id;

        @Column(name = "created_on", columnDefinition = "datetime2")
        private LocalDateTime createdOn;

        @Column(name = "updated_on", columnDefinition = "datetimeoffset")
        private OffsetDateTime updatedOn;

        @Column(name = "last_accessed_on", columnDefinition = "smalldatetime")
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
