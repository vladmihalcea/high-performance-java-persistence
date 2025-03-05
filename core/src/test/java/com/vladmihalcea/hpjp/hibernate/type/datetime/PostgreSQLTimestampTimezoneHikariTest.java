package com.vladmihalcea.hpjp.hibernate.type.datetime;

import com.vladmihalcea.hpjp.util.AbstractTest;
import com.vladmihalcea.hpjp.util.providers.Database;
import com.zaxxer.hikari.HikariConfig;
import org.hibernate.Session;
import org.hibernate.cfg.AvailableSettings;
import org.junit.Test;

import javax.sql.DataSource;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.Properties;

import static org.junit.Assert.assertEquals;

/**
 * @author Vlad Mihalcea
 */
public class PostgreSQLTimestampTimezoneHikariTest extends AbstractTest {

    @Override
    protected Class<?>[] entities() {
        return new Class<?>[] {
        };
    }

    @Override
    protected void additionalProperties(Properties properties) {
        properties.setProperty(AvailableSettings.HBM2DDL_AUTO, "none");
    }

    protected boolean connectionPooling() {
        return true;
    }

    protected HikariConfig hikariConfig(DataSource dataSource) {
        HikariConfig hikariConfig = super.hikariConfig(dataSource);
        hikariConfig.setMaximumPoolSize(1);
        hikariConfig.setConnectionInitSql("SET timezone TO 'UTC'");
        return hikariConfig;
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
                    assertEquals("UTC", timeZone);

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
}
