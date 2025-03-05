package com.vladmihalcea.hpjp.hibernate.type.datetime;

import com.vladmihalcea.hpjp.util.AbstractTest;
import com.vladmihalcea.hpjp.util.providers.Database;
import org.hibernate.Session;
import org.hibernate.cfg.AvailableSettings;
import org.junit.Test;

import java.sql.ResultSet;
import java.sql.Statement;
import java.util.Properties;
import java.util.TimeZone;

import static org.junit.Assert.assertEquals;

/**
 * @author Vlad Mihalcea
 */
public class PostgreSQLTimestampTimezoneTest extends AbstractTest {

    @Override
    protected Class<?>[] entities() {
        return new Class<?>[] {
        };
    }

    @Override
    protected void additionalProperties(Properties properties) {
        properties.setProperty(AvailableSettings.HBM2DDL_AUTO, "none");
    }

    @Override
    protected Database database() {
        return Database.POSTGRESQL;
    }

    @Override
    protected void beforeInit() {
        executeStatement("drop table if exists event cascade");
        executeStatement("""
            CREATE TABLE event (
                id integer not null,
                timestamp_without_tz timestamp without time zone,
                timestamp_with_tz timestamp with time zone,
                PRIMARY KEY (id)
            )
            """
        );
    }

    @Test
    public void testDefaultSessionTimezone() {
        doInJPA(entityManager -> {
            entityManager.unwrap(Session.class).doWork(connection -> {
                try(Statement statement = connection.createStatement()) {
                    ResultSet resultSet = statement.executeQuery("""
                        SELECT setting, source, context
                        FROM pg_settings
                        WHERE name = 'TimeZone'
                        """);

                    LOGGER.info("{}{}", System.lineSeparator(), resultSetToString(resultSet));
                }
            });
        });

        doInJPA(entityManager -> {
            entityManager.unwrap(Session.class).doWork(connection -> {
                try(Statement statement = connection.createStatement()) {
                    statement.execute("""
                        INSERT INTO event (
                            id, timestamp_without_tz, timestamp_with_tz) 
                        VALUES (
                            1, '2031-12-10 07:30:45.0', '2031-12-10 07:30:45.0')
                        """);
                }
            });
        });

        doInJPA(entityManager -> {
            entityManager.unwrap(Session.class).doWork(connection -> {
                try(Statement statement = connection.createStatement()) {
                    String timeZone = selectStringColumn(connection, "SELECT current_setting('timezone')");
                    assertEquals(TimeZone.getDefault().getID(), timeZone);

                    ResultSet resultSet = statement.executeQuery("""
                        SELECT timestamp_without_tz, timestamp_with_tz 
                        FROM event
                        WHERE id = 1
                        """);

                    LOGGER.info("{}{}", System.lineSeparator(), resultSetToString(resultSet));
                }
            });
        });
    }

    @Test
    public void testDefaultSessionTimezoneOnRead() {
        doInJPA(entityManager -> {
            entityManager.unwrap(Session.class).doWork(connection -> {
                try(Statement statement = connection.createStatement()) {
                    statement.execute("SET timezone TO 'Asia/Tokyo'");

                    statement.execute("""
                        INSERT INTO event (
                            id, timestamp_without_tz, timestamp_with_tz) 
                        VALUES (
                            1, '2031-12-10 07:30:45.0', '2031-12-10 07:30:45.0')
                        """);
                }
            });
        });

        doInJPA(entityManager -> {
            entityManager.unwrap(Session.class).doWork(connection -> {
                try(Statement statement = connection.createStatement()) {
                    String timeZone = selectStringColumn(connection, "SELECT current_setting('timezone')");
                    assertEquals(TimeZone.getDefault().getID(), timeZone);

                    ResultSet resultSet = statement.executeQuery("""
                        SELECT timestamp_without_tz, timestamp_with_tz 
                        FROM event
                        WHERE id = 1
                        """);

                    LOGGER.info("{}{}", System.lineSeparator(), resultSetToString(resultSet));
                }
            });
        });
    }

    @Test
    public void testExplicitSessionTimezone() {
        doInJPA(entityManager -> {
            entityManager.unwrap(Session.class).doWork(connection -> {
                try(Statement statement = connection.createStatement()) {
                    statement.execute("SET timezone TO 'Asia/Tokyo'");

                    statement.execute("""
                        INSERT INTO event (
                            id, timestamp_without_tz, timestamp_with_tz) 
                        VALUES (
                            1, '2031-12-10 07:30:45.0', '2031-12-10 07:30:45.0')
                        """);
                }
            });
        });

        doInJPA(entityManager -> {
            entityManager.unwrap(Session.class).doWork(connection -> {
                try(Statement statement = connection.createStatement()) {
                    statement.execute("SET timezone TO 'UTC'");

                    assertEquals(
                        "UTC",
                        selectStringColumn(connection, "SELECT current_setting('timezone')")
                    );

                    ResultSet resultSet = statement.executeQuery("""
                        SELECT timestamp_without_tz, timestamp_with_tz 
                        FROM event
                        WHERE id = 1
                        """);

                    while (resultSet.next()) {
                        String timestampWithoutTimezone = resultSet.getString(1);
                        String timestampWithTimezone = resultSet.getString(2);

                        LOGGER.info("Timestamp without time zone: {}", timestampWithoutTimezone);
                        LOGGER.info("Timestamp with time zone: {}", timestampWithTimezone);
                    }
                }
            });
        });
    }
}
