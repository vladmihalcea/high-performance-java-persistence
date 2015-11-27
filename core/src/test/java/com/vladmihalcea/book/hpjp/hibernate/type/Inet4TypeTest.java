package com.vladmihalcea.book.hpjp.hibernate.type;

import com.sun.org.apache.xpath.internal.operations.Bool;
import com.vladmihalcea.book.hpjp.util.AbstractPostgreSQLIntegrationTest;
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
public class Inet4TypeTest extends AbstractPostgreSQLIntegrationTest {

    @Override
    protected Class<?>[] entities() {
        return new Class<?>[] {
            Ipv4.class
        };
    }

    @Override
    public void init() {
        super.init();
        doInJDBC(connection -> {
            try (
                    Statement statement = connection.createStatement();
            ) {

                statement.executeUpdate("CREATE INDEX ON Ipv4 USING gist (ip inet_ops)");
            } catch (SQLException e) {
                fail(e.getMessage());
            }
        });
    }

    @Test
    public void test() {
        doInJPA(entityManager -> {
            entityManager.persist(new Ipv4("192.168.0.123"));
        });
        doInJPA(entityManager -> {
            Ipv4 inet4 = entityManager.createQuery("select ip from Ipv4 ip", Ipv4.class).getSingleResult();
            assertEquals("192.168.0.123", inet4.getValue().getAddress());

            assertTrue((Boolean) entityManager.createNativeQuery("select ip && inet '192.168.0.1/24' from Ipv4").getSingleResult());
        });
    }

    @Entity(name = "Ipv4")
    public static class Ipv4 {

        @Id
        @GeneratedValue
        private Long id;

        @Type(type = "com.vladmihalcea.book.hpjp.hibernate.type.Inet4Type")
        @Column(name = "ip", columnDefinition = "inet")
        private Inet4 value;

        public Ipv4() {}

        public Ipv4(String value) {
            this.value = new Inet4(value);
        }

        public Inet4 getValue() {
            return value;
        }
    }
}
