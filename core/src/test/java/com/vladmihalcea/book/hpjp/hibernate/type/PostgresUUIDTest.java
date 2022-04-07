package com.vladmihalcea.book.hpjp.hibernate.type;

import com.vladmihalcea.book.hpjp.util.AbstractPostgreSQLIntegrationTest;
import org.hibernate.Session;
import org.hibernate.annotations.Generated;
import org.hibernate.annotations.GenerationTime;
import org.junit.Test;

import jakarta.persistence.*;
import java.util.Properties;
import java.util.UUID;

import static org.junit.Assert.assertNotNull;

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

        @Generated(GenerationTime.INSERT)
        @Column(columnDefinition = "UUID NOT NULL DEFAULT uuid_generate_v4()", insertable = false)
        private UUID uuid;

    }
}
