package com.vladmihalcea.hpjp.hibernate.type;

import com.vladmihalcea.hpjp.util.AbstractPostgreSQLIntegrationTest;
import jakarta.persistence.*;
import org.hibernate.annotations.Generated;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.hibernate.generator.EventType.INSERT;
import static org.junit.jupiter.api.Assertions.assertNotNull;

/**
 * @author Vlad Mihalcea
 */
public class PostgresUUIDTest extends AbstractPostgreSQLIntegrationTest {

    @Override
    protected Class<?>[] entities() {
        return new Class<?>[] {
                Event.class
        };
    }

    @Override
    public void beforeInit() {
        executeStatement("CREATE EXTENSION IF NOT EXISTS \"uuid-ossp\"");
    }

    @Override
    public void afterDestroy() {
        executeStatement("DROP EXTENSION \"uuid-ossp\" CASCADE");
    }

    @Test
    public void test() {
        Event _event = doInJPA(entityManager -> {
            Event event = new Event();
            entityManager.persist(event);
            return event;
        });

        assertNotNull(_event.uuid);
    }

    @Entity(name = "Event")
    @Table(name = "event")
    public static class Event {

        @Id
        @GeneratedValue
        private Long id;

        @Generated(event = {INSERT})
        @Column(columnDefinition = "UUID NOT NULL DEFAULT uuid_generate_v4()", insertable = false)
        private UUID uuid;

    }
}
