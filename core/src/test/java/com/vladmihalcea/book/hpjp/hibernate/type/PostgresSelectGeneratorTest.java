package com.vladmihalcea.book.hpjp.hibernate.type;

import com.vladmihalcea.book.hpjp.util.AbstractPostgreSQLIntegrationTest;
import org.hibernate.annotations.GenericGenerator;
import org.hibernate.annotations.NaturalId;
import org.junit.Test;

import javax.persistence.*;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.Assert.assertNotNull;

/**
 * @author Vlad Mihalcea
 */
public class PostgresSelectGeneratorTest extends AbstractPostgreSQLIntegrationTest {

    @Override
    protected Class<?>[] entities() {
        return new Class<?>[] {
                Event.class
        };
    }

    @Override
    public void init() {
        ddl("CREATE SEQUENCE event_sequence START 1");
        super.init();
    }

    @Override
    public void destroy() {
        super.destroy();
        ddl("DROP SEQUENCE event_sequence");
    }

    @Test
    public void test() {
        Long id = doInJPA(entityManager -> {
            Event event = new Event();
            event.name = "Hypersistence";
            entityManager.persist(event);

            return event.id;
        });

        assertNotNull(id);
    }

    @Entity(name = "Event")
    @Table(name = "event")
    public static class Event {

        @Id
        @GeneratedValue(generator = "select")
        @GenericGenerator(name = "select", strategy = "select")
        @Column(columnDefinition = "BIGINT DEFAULT nextval('event_sequence')")
        private Long id;

        @NaturalId
        private String name;

    }
}
