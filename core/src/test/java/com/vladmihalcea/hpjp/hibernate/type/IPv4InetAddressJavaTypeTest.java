package com.vladmihalcea.hpjp.hibernate.type;

import com.vladmihalcea.hpjp.util.AbstractPostgreSQLIntegrationTest;
import jakarta.persistence.*;
import org.hibernate.HibernateException;
import org.hibernate.annotations.JavaType;
import org.hibernate.type.descriptor.java.InetAddressJavaType;
import org.junit.Test;

import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.List;

import static org.junit.Assert.*;

/**
 * @author Vlad Mihalcea
 */
public class IPv4InetAddressJavaTypeTest extends AbstractPostgreSQLIntegrationTest {

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
                    .setIp(toInetAddress("192.168.0.123/24"))
            );
        });
    }

    @Test
    public void testFindById() {
        doInJPA(entityManager -> {
            Event event = entityManager.find(Event.class, 1L);

            assertEquals("192.168.0.123", event.getIp().getHostAddress());

            event.setIp(toInetAddress("192.168.0.231/24"));

            return event;
        });

        doInJPA(entityManager -> {
            Event event = entityManager.find(Event.class, 1L);

            assertEquals("192.168.0.231", event.getIp().getHostAddress());
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
            assertEquals("192.168.0.123", events.get(0).getIp().getHostAddress());
        });
    }

    @Entity(name = "Event")
    @Table(name = "event")
    public static class Event {

        @Id
        private Long id;

        @JavaType(InetAddressJavaType.class)
        @Column(name = "ip", columnDefinition = "inet")
        private InetAddress ip;

        public Long getId() {
            return id;
        }

        public Event setId(Long id) {
            this.id = id;
            return this;
        }

        public InetAddress getIp() {
            return ip;
        }

        public Event setIp(InetAddress ip) {
            this.ip = ip;
            return this;
        }
    }

    public static InetAddress toInetAddress(String address) {
        try {
            String host = address.replaceAll("\\/.*$", "");
            return Inet4Address.getByName(host);
        } catch (UnknownHostException e) {
            throw new HibernateException(
                new IllegalArgumentException(e)
            );
        }
    }
}
