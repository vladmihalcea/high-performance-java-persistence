package com.vladmihalcea.book.hpjp.hibernate.type;

import com.vladmihalcea.book.hpjp.util.AbstractPostgreSQLIntegrationTest;
import org.hibernate.Session;
import org.hibernate.annotations.Type;
import org.junit.Test;

import javax.persistence.*;

import java.sql.*;

import static org.junit.Assert.*;

/**
 * <code>Inet4TypeTest</code> -
 *
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

                statement.executeUpdate("CREATE INDEX ON Event USING gist (ip inet_ops)");
            } catch (SQLException e) {
                fail(e.getMessage());
            }
        });
    }

    @Test
    public void test() {
        doInJPA(entityManager -> {
            entityManager.persist(new Event("192.168.0.123"));
        });
        doInJPA(entityManager -> {
            Event event = entityManager.createQuery("select e from Event e", Event.class).getSingleResult();
            assertEquals("192.168.0.123", event.getIp().getAddress());

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

            Event matchingEvent = (Event) entityManager.
                    createNativeQuery(
                        "select {e.*} " +
                        "from Event e " +
                        "where " +
                        "   e.ip && CAST(:network AS inet) = TRUE"
                    , Event.class).
                    setParameter("network", "192.168.0.1/24").
                    getSingleResult();
            assertSame(matchingEvent, event);
        });
    }

    @Entity(name = "Event")
    public static class Event {

        @Id
        @GeneratedValue
        private Long id;

        @Type(type = "com.vladmihalcea.book.hpjp.hibernate.type.IPv4Type")
        @Column(name = "ip", columnDefinition = "inet")
        private IPv4 ip;

        public Event() {}

        public Event(String address) {
            this.ip = new IPv4(address);
        }

        public IPv4 getIp() {
            return ip;
        }
    }
}
