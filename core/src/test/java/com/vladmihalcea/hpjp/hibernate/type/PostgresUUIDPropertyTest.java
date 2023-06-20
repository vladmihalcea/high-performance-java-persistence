package com.vladmihalcea.hpjp.hibernate.type;

import com.vladmihalcea.hpjp.util.AbstractPostgreSQLIntegrationTest;
import jakarta.persistence.*;
import org.junit.Test;

import java.util.UUID;

import static org.junit.Assert.assertNotNull;

/**
 * @author Vlad Mihalcea
 */
public class PostgresUUIDPropertyTest extends AbstractPostgreSQLIntegrationTest {

    @Override
    protected Class<?>[] entities() {
        return new Class<?>[] {
            Event.class
        };
    }

    @Override
    public void init() {
        executeStatement("CREATE EXTENSION IF NOT EXISTS \"uuid-ossp\"");
        super.init();
    }

    @Override
    public void destroy() {
        doInJPA(entityManager -> {
            entityManager.createNativeQuery(
                "DROP EXTENSION \"uuid-ossp\" CASCADE"
            ).executeUpdate();
        });
        super.destroy();
    }

    @Test
    public void test() {
        doInJPA(entityManager -> {
            Event event = new Event()
                .setId(1L)
                .setExternalId(UUID.randomUUID());

            entityManager.persist(event);
            return event;
        });

        doInJPA(entityManager -> {
            Event event = entityManager.find(Event.class, 1L);
            assertNotNull(event.getExternalId());
        });
    }

    @Entity(name = "Event")
    @Table(name = "event")
    public static class Event {

        @Id
        private Long id;

        @Column(
            name = "external_id",
            columnDefinition = "UUID NOT NULL"
        )
        private UUID externalId;

        public Long getId() {
            return id;
        }

        public Event setId(Long id) {
            this.id = id;
            return this;
        }

        public UUID getExternalId() {
            return externalId;
        }

        public Event setExternalId(UUID externalId) {
            this.externalId = externalId;
            return this;
        }
    }
}
