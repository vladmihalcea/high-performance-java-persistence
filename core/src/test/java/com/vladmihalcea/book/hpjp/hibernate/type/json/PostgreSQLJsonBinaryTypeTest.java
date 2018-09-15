package com.vladmihalcea.book.hpjp.hibernate.type.json;

import com.vladmihalcea.book.hpjp.hibernate.type.json.model.BaseEntity;
import com.vladmihalcea.book.hpjp.hibernate.type.json.model.Location;
import com.vladmihalcea.book.hpjp.hibernate.type.json.model.Ticket;
import com.vladmihalcea.book.hpjp.util.AbstractPostgreSQLIntegrationTest;
import com.vladmihalcea.book.hpjp.util.providers.DataSourceProvider;
import com.vladmihalcea.book.hpjp.util.providers.Database;
import com.vladmihalcea.book.hpjp.util.providers.PostgreSQLDataSourceProvider;
import org.hibernate.SessionFactory;
import org.hibernate.annotations.Type;
import org.hibernate.boot.MetadataBuilder;
import org.hibernate.boot.model.TypeContributions;
import org.hibernate.boot.model.TypeContributor;
import org.hibernate.boot.spi.MetadataBuilderContributor;
import org.hibernate.dialect.Dialect;
import org.hibernate.dialect.function.StandardSQLFunction;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.service.ServiceRegistry;
import org.hibernate.type.StandardBasicTypes;
import org.junit.Test;

import javax.persistence.*;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.Assert.assertEquals;

/**
 * @author Vlad Mihalcea
 */
public class PostgreSQLJsonBinaryTypeTest extends AbstractPostgreSQLIntegrationTest {

    @Override
    protected Class<?>[] entities() {
        return new Class<?>[] {
            Event.class,
            Participant.class
        };
    }

    @Override
    protected void afterInit() {
        doInJPA(entityManager -> {
            Event nullEvent = new Event();
            nullEvent.setId(0L);
            entityManager.persist(nullEvent);

            Location location = new Location();
            location.setCountry("Romania");
            location.setCity("Cluj-Napoca");

            Event event = new Event();
            event.setId(1L);
            event.setLocation(location);
            entityManager.persist(event);

            Ticket ticket = new Ticket();
            ticket.setPrice(12.34d);
            ticket.setRegistrationCode("ABC123");

            Participant participant = new Participant();
            participant.setId(1L);
            participant.setTicket(ticket);
            participant.setEvent(event);

            entityManager.persist(participant);
        });
    }

    @Test
    public void test() {
        doInJPA(entityManager -> {
            Event event = entityManager.find(Event.class, 1L);
            assertEquals("Cluj-Napoca", event.getLocation().getCity());

            Participant participant = entityManager.find(Participant.class, 1L);
            assertEquals("ABC123", participant.getTicket().getRegistrationCode());

            List<String> participants = entityManager.createNativeQuery(
                "select jsonb_pretty(p.ticket) " +
                "from participant p " +
                "where p.ticket ->> 'price' > :price")
            .setParameter("price", "10")
            .getResultList();

            event.getLocation().setCity("Constanța");
            assertEquals(Integer.valueOf(0), event.getVersion());
            entityManager.flush();
            assertEquals(Integer.valueOf(1), event.getVersion());

            assertEquals(1, participants.size());
        });

        doInJPA(entityManager -> {
            Event event = entityManager.find(Event.class, 1L);
            event.getLocation().setCity(null);
            assertEquals(Integer.valueOf(1), event.getVersion());
            entityManager.flush();
            assertEquals(Integer.valueOf(2), event.getVersion());
        });

        doInJPA(entityManager -> {
            Event event = entityManager.find(Event.class, 1L);
            event.setLocation(null);
            assertEquals(Integer.valueOf(2), event.getVersion());
            entityManager.flush();
            assertEquals(Integer.valueOf(3), event.getVersion());
        });
    }

    @Entity(name = "Event")
    @Table(name = "event")
    public static class Event extends BaseEntity {

        @Type(type = "jsonb")
        @Column(columnDefinition = "jsonb")
        private Location location;

        public Location getLocation() {
            return location;
        }

        public void setLocation(Location location) {
            this.location = location;
        }
    }

    @Entity(name = "Participant")
    @Table(name = "participant")
    public static class Participant extends BaseEntity {

        @Type(type = "jsonb")
        @Column(columnDefinition = "jsonb")
        private Ticket ticket;

        @ManyToOne
        private Event event;

        public Ticket getTicket() {
            return ticket;
        }

        public void setTicket(Ticket ticket) {
            this.ticket = ticket;
        }

        public Event getEvent() {
            return event;
        }

        public void setEvent(Event event) {
            this.event = event;
        }
    }
}
