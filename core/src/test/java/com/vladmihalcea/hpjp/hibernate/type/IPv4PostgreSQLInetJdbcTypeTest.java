package com.vladmihalcea.hpjp.hibernate.type;

import com.vladmihalcea.hpjp.util.AbstractPostgreSQLIntegrationTest;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import org.hibernate.annotations.JdbcType;
import org.hibernate.dialect.PostgreSQLInetJdbcType;
import org.junit.Test;

import java.sql.SQLException;
import java.sql.Statement;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

/**
 * @author Vlad Mihalcea
 */
public class IPv4PostgreSQLInetJdbcTypeTest extends AbstractPostgreSQLIntegrationTest {

    @Override
    protected Class<?>[] entities() {
        return new Class<?>[] {
            Event.class
        };
    }

    @Override
    public void afterInit() {
        doInJDBC(connection -> {
            try (Statement statement = connection.createStatement()) {
                statement.executeUpdate("CREATE INDEX ON event USING gist (ip inet_ops)");
            } catch (SQLException e) {
                fail(e.getMessage());
            }
        });

        doInJPA(entityManager -> {
            entityManager.persist(
                new Event()
                    .setId(1L)
                    .setIp("192.168.0.123")
            );
        });
    }

    @Test
    public void testFindById() {
        doInJPA(entityManager -> {
            Event event = entityManager.find(Event.class, 1L);

            assertEquals("192.168.0.123", event.getIp());

            event.setIp("192.168.0.231");

            return event;
        });

        doInJPA(entityManager -> {
            Event event = entityManager.find(Event.class, 1L);

            assertEquals("192.168.0.231", event.getIp());
        });
    }

    @Test
    public void testNativeQuery() {
        doInJPA(entityManager -> {
            List<Event> events = entityManager.createNativeQuery("""
                SELECT e.*
                FROM event e
                WHERE
                   e.ip && CAST(:network AS inet) = true
                """, Event.class)
            .setParameter("network", "192.168.0.1/24")
            .getResultList();

            assertEquals(1, events.size());
            assertEquals("192.168.0.123", events.get(0).getIp());
        });
    }

    @Entity(name = "Event")
    @Table(name = "event")
    public static class Event {

        @Id
        private Long id;

        @JdbcType(PostgreSQLInetJdbcType.class)
        @Column(name = "ip", columnDefinition = "inet")
        private String ip;

        public Long getId() {
            return id;
        }

        public Event setId(Long id) {
            this.id = id;
            return this;
        }

        public String getIp() {
            return ip;
        }

        public Event setIp(String ip) {
            this.ip = ip;
            return this;
        }
    }
}
