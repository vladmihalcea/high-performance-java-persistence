package com.vladmihalcea.book.hpjp.hibernate.type;

import com.vladmihalcea.book.hpjp.util.AbstractPostgreSQLIntegrationTest;
import io.hypersistence.utils.hibernate.type.basic.Inet;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.Persistence;
import jakarta.persistence.Table;
import org.hibernate.Session;
import org.junit.jupiter.api.Test;

import java.time.Period;

import static jakarta.persistence.GenerationType.IDENTITY;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

/**
 * Simple test case to demonstrate problem with automatic schema creation when
 * more than one properties are of type with SQL type OTHER.
 * In my case, this is more of "academic" interest, in real life situation I will
 * use columnDefinition attribute (like those commented-out) or, more likely, avoid
 * automatic schema generation at all. Especially will avoid updating schema that
 * has been created using pre-6 version of Hibernate.
 */

public class PostgreSQLDdlCreateOtherTest extends AbstractPostgreSQLIntegrationTest {

    @Override
    protected Class<?>[] entities() {
        return new Class<?>[]{
                P.class
        };
    }

    @Test
    void test() {
        final var factory = Persistence.createEntityManagerFactory("manager");
        final var manager = factory.createEntityManager();
        final var tx = manager.unwrap(Session.class).beginTransaction();

        final var p1 = new P();
        p1.setPeriod(Period.ofWeeks(3));
        p1.setInet(new Inet("192.168.56.42"));
        manager.persist(p1);
        tx.commit();

        final var actual = manager.find(P.class, p1.getId());
        assertNotNull(actual);
        assertEquals(p1.getInet(), actual.getInet());
        assertEquals(p1.getPeriod(), actual.getPeriod());
    }

    @Entity(name = "pe")
    @Table(name = "pe")
    public static class P {
        @Id
        @GeneratedValue(strategy = IDENTITY)
        @Column(name = "id", nullable = false, updatable = false)
        private Integer id;

        @Column/*(columnDefinition = "interval")*/
        private Period period;

        @Column/*(columnDefinition = "inet")*/
        private Inet inet;

        public Integer getId() {
            return id;
        }

        public void setId(Integer id) {
            this.id = id;
        }

        public Period getPeriod() {
            return period;
        }

        public void setPeriod(Period period) {
            this.period = period;
        }

        public Inet getInet() {
            return inet;
        }

        public void setInet(Inet inet) {
            this.inet = inet;
        }
    }

}
