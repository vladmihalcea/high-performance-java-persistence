package com.vladmihalcea.book.hpjp.hibernate.type;

import com.vladmihalcea.book.hpjp.util.AbstractPostgreSQLIntegrationTest;
import org.hibernate.Session;
import org.hibernate.annotations.Type;
import org.hibernate.annotations.TypeDef;

import org.junit.Test;

import javax.persistence.*;
import java.net.UnknownHostException;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

/**
 * @author Vlad Mihalcea
 */
public class IPv4TypeTest extends AbstractPostgreSQLIntegrationTest {

    @Override
    protected Class<?>[] entities() {
        return new Class<?>[] {
            Event.class
        };
    }

    @Override
    public void init() {
        super.init();
        doInJDBC(connection -> {
            try (
                    Statement statement = connection.createStatement();
            ) {
                statement.executeUpdate("CREATE INDEX ON event USING gist (ip inet_ops)");
            } catch (SQLException e) {
                fail(e.getMessage());
            }
        });
    }

    @Test
    public void test() {
        final AtomicReference<Event> eventHolder = new AtomicReference<>();
        doInJPA(entityManager -> {
            entityManager.persist(new Event());
            Event event = new Event("192.168.0.231");
            entityManager.persist(event);
            eventHolder.set(event);
        });
        doInJPA(entityManager -> {
            Event event = entityManager.find(Event.class, eventHolder.get().getId());
            event.setIp("192.168.0.123");
        });
        doInJPA(entityManager -> {
            Event event = entityManager.createQuery("select e from Event e where ip is not null", Event.class).getSingleResult();
            assertEquals("192.168.0.123", event.getIp().getAddress());

            try {
                assertEquals("192.168.0.123", event.getIp().toInetAddress().getHostAddress());
            } catch (UnknownHostException e) {
                fail(e.getMessage());
            }

            Session session = entityManager.unwrap(Session.class);
            session.doWork(connection -> {
                try(PreparedStatement ps = connection.prepareStatement(
                        "select * " +
                        "from Event e " +
                        "where " +
                        "   e.ip && ?::inet = TRUE"
                )) {
                    ps.setObject(1, "192.168.0.1/24");
                    ResultSet rs = ps.executeQuery();
                    while(rs.next()) {
                        Long id = rs.getLong(1);
                        String ip = rs.getString(2);
                        assertEquals("192.168.0.123", ip);
                    }
                }
            });

            Event matchingEvent = (Event) entityManager.createNativeQuery(
                "SELECT {e.*} " +
                "FROM event e " +
                "WHERE " +
                "   e.ip && CAST(:network AS inet) = TRUE", Event.class)
            .setParameter("network", "192.168.0.1/24")
            .getSingleResult();
            assertEquals("192.168.0.123", matchingEvent.getIp().getAddress());
        });
    }

    @Entity(name = "Event")
    @Table(name = "event")
    @TypeDef( name = "ipv4", typeClass = IPv4Type.class, defaultForType = IPv4.class )
    public static class Event {

        @Id
        @GeneratedValue
        private Long id;

        //@Type(type = "com.vladmihalcea.book.hpjp.hibernate.type.IPv4Type")
        @Column(name = "ip", columnDefinition = "inet")
        private IPv4 ip;

        public Event() {}

        public Event(String address) {
            this.ip = new IPv4(address);
        }

        public Long getId() {
            return id;
        }

        public IPv4 getIp() {
            return ip;
        }

        public void setIp(String address) {
            this.ip = new IPv4(address);
        }
    }
}
