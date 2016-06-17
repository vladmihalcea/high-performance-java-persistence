package com.vladmihalcea.book.hpjp.hibernate.type.json;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.vladmihalcea.book.hpjp.util.AbstractPostgreSQLIntegrationTest;
import org.hibernate.annotations.Type;
import org.hibernate.annotations.TypeDef;
import org.junit.Test;

import javax.persistence.*;
import java.io.Serializable;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.Assert.assertEquals;

/**
 * @author Vlad Mihalcea
 */
public class PostgreSQLJsonTypeTest extends AbstractPostgreSQLIntegrationTest {

    @Override
    protected Class<?>[] entities() {
        return new Class<?>[] {
            Event.class,
            Participant.class
        };
    }

    @Test
    public void test() {
        final AtomicReference<Event> eventHolder = new AtomicReference<>();
        final AtomicReference<Participant> participantHolder = new AtomicReference<>();

        doInJPA(entityManager -> {
            entityManager.persist(new Event());

            Location location = new Location();
            location.setCountry("Romania");
            location.setCity("Cluj-Napoca");
            Event event = new Event();
            event.setValue(location);

            Ticket ticket = new Ticket();
            ticket.setPrice(12.34d);
            ticket.setRegistrationCode("ABC123");
            Participant participant = new Participant();
            participant.setTicket(ticket);

            entityManager.persist(event);
            entityManager.persist(participant);

            eventHolder.set(event);
            participantHolder.set(participant);
        });
        doInJPA(entityManager -> {
            Event event = entityManager.find(Event.class, eventHolder.get().getId());
            assertEquals("Cluj-Napoca", event.getLocation().getCity());

            Participant participant = entityManager.find(Participant.class, participantHolder.get().getId());
            assertEquals("ABC123", participant.getTicket().getRegistrationCode());

            List<String> participants = entityManager.createNativeQuery(
                "select jsonb_pretty(p.ticket) " +
                "from participant p " +
                "where p.ticket ->> 'price' > '10'")
            .getResultList();

            assertEquals(1, participants.size());
        });
    }

    public static class Location implements Serializable {

        private String country;

        private String city;

        public String getCountry() {
            return country;
        }

        public void setCountry(String country) {
            this.country = country;
        }

        public String getCity() {
            return city;
        }

        public void setCity(String city) {
            this.city = city;
        }
    }

    public static class Ticket implements Serializable {

        private String registrationCode;

        private double price;

        public String getRegistrationCode() {
            return registrationCode;
        }

        public void setRegistrationCode(String registrationCode) {
            this.registrationCode = registrationCode;
        }

        public double getPrice() {
            return price;
        }

        public void setPrice(double price) {
            this.price = price;
        }
    }

    @Entity(name = "Event")
    @Table(name = "event")
    public static class Event extends BaseEntity {

        @Id
        @GeneratedValue
        private Long id;

        @Type(type = "json")
        @Column(columnDefinition = "jsonb")
        protected ObjectNode location;

        public Event() {}

        public Long getId() {
            return id;
        }

        public Location getLocation() {
            return toPojo(location, Location.class);
        }

        public void setValue(Object location) {
            this.location = toJson(location);
        }
    }

    @Entity(name = "Participant")
    @Table(name = "participant")
    public static class Participant extends BaseEntity {

        @Id
        @GeneratedValue
        private Long id;

        @Type(type = "json")
        @Column(columnDefinition = "jsonb")
        protected ObjectNode ticket;

        public Long getId() {
            return id;
        }

        public Ticket getTicket() {
            return toPojo(ticket, Ticket.class);
        }

        public void setTicket(Ticket ticket) {
            this.ticket = toJson(ticket);
        }
    }

    @TypeDef(name = "json", typeClass = PostgreSQLJsonType.class)
    @MappedSuperclass
    public static class BaseEntity {

        private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

        protected <T> T toPojo(ObjectNode json, Class<T> clazz) {
            try {
                return OBJECT_MAPPER.treeToValue(json, clazz);
            } catch (JsonProcessingException e) {
                throw new IllegalArgumentException("Json object " + json + " cannot be transformed to a " + clazz + " object type");
            }
        }

        protected ObjectNode toJson(Object value) {
            return OBJECT_MAPPER.valueToTree(value);
        }
    }
}
