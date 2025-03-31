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
public class OracleTimestampWithTimeZoneVsLocalTest extends AbstractTest {

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
    public void testTimestampColumn() {
        doInJPA(entityManager -> {
            entityManager.createNativeQuery("""
                INSERT INTO event (id, created_on) 
                VALUES (1, TIMESTAMP '2031-12-10 07:30:45.123456')
                """)
            .executeUpdate();
        });

        doInJPA(entityManager -> {
            Event event = entityManager.find(Event.class, 1);

            assertEquals(
                LocalDateTime.parse("2031-12-10T07:30:45.123456"),
                event.getCreatedOn()
            );
        });
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
                SET updated_on = TIMESTAMP '2031-12-10 07:30:45.987654321 Europe/Paris'
                WHERE id = 1
                """)
            .executeUpdate();

            assertEquals(1, updateCount);

            Event event = entityManager.find(Event.class, 1);

            assertEquals(
                ZonedDateTime.of(2031, 12, 10, 7, 30, 45, 987654321, ZoneId.of("Europe/Paris")),
                event.getUpdatedOn()
            );

            event.setUpdatedOn(
                ZonedDateTime.of(2031, 12, 10, 7, 30, 45, 987654321, ZoneId.of("Europe/London"))
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
            int updateCount = entityManager.createNativeQuery("""
                UPDATE event
                SET last_accessed_on = TIMESTAMP '2031-12-10 07:30:45.987654321 Europe/Paris'
                WHERE id = 1
                """)
            .executeUpdate();

            assertEquals(1, updateCount);
        });

        doInJPA(entityManager -> {
            entityManager.unwrap(Session.class).doWork(connection -> {
                try(Statement statement = connection.createStatement()) {
                    ResultSet resultSet = statement.executeQuery("""
                        SELECT
                            TZ_OFFSET('Europe/Paris') as Paris_Zone_Offset,
                            TZ_OFFSET(SESSIONTIMEZONE) as Our_Zone_Offset
                        FROM dual
                        """);

                    LOGGER.info("{}{}", System.lineSeparator(), resultSetToString(resultSet));
                }

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

        doInJPA(entityManager -> {
            entityManager.unwrap(Session.class).doWork(connection -> {
                executeStatement(connection, "ALTER SESSION SET time_zone='Europe/Paris'");

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

        doInJPA(entityManager -> {
            Event event = entityManager.find(Event.class, 1);

            assertEquals(
                ZonedDateTime.of(2031, 12, 10, 7, 30, 45, 9876543, ZoneId.of("Europe/Paris"))
                    .withZoneSameInstant(ZoneId.systemDefault()).toOffsetDateTime(),
                event.getLastAccessedOn()
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

        @JdbcType(ZonedDateTimeJdbcType.class)
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

        public LocalDateTime getCreatedOn() {
            return createdOn;
        }

        public Event setCreatedOn(LocalDateTime dateProperty) {
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
