package com.vladmihalcea.book.hpjp.hibernate.association;

import com.vladmihalcea.book.hpjp.util.AbstractTest;
import org.junit.Test;

import jakarta.persistence.*;

import static org.junit.Assert.assertEquals;

/**
 * @author Vlad Mihalcea
 */
public class TwoMapsIdsTest extends AbstractTest {

    @Override
    protected Class<?>[] entities() {
        return new Class<?>[] {
            Address.class,
            Location.class,
            Person.class,
        };
    }

    @Test
    public void test() {
        doInJPA(entityManager -> {
            Address address = new Address();
            address.id = 1L;

            Location location = new Location();
            location.id = 1L;

            Person person = new Person();
            person.personId = 1L;
            person.address = address;
            person.location = location;

            entityManager.persist(address);
            entityManager.persist(location);
            entityManager.persist(person);
        });

        doInJPA(entityManager -> {
            Person person = entityManager.find(Person.class, 1L);
            assertEquals(Long.valueOf(1L), person.address.id);
            assertEquals(Long.valueOf(1L), person.location.id);
        });
    }

    @Entity(name = "Address")
    public static class Address {

        @Id
        private Long id;
    }

    @Entity(name = "Location")
    public static class Location {

        @Id
        private Long id;
    }

    @Entity(name = "Person")
    public static class Person  {

        @Id
        private Long personId;

        @OneToOne
        @MapsId
        @JoinColumn(name = "personId")
        private Address address;

        @OneToOne
        @MapsId
        @JoinColumn(name = "personId")
        private Location location;
    }
}
