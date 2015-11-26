package com.vladmihalcea.book.hpjp.hibernate.type;

import com.vladmihalcea.book.hpjp.util.AbstractPostgreSQLIntegrationTest;
import org.hibernate.annotations.Type;
import org.junit.Test;

import javax.persistence.*;

import static org.junit.Assert.assertEquals;

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

    @Test
    public void test() {
        doInJPA(entityManager -> {
            entityManager.persist(new Ipv4("127.0.0.1"));
        });
        doInJPA(entityManager -> {
            Ipv4 inet4 = entityManager.createQuery("select ip from Ipv4 ip", Ipv4.class).getSingleResult();
            assertEquals("127.0.0.1", inet4.getValue().getAddress());
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
