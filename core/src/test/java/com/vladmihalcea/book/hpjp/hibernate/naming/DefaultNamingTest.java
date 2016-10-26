package com.vladmihalcea.book.hpjp.hibernate.naming;

import com.vladmihalcea.book.hpjp.util.AbstractOracleXEIntegrationTest;
import org.junit.Test;

import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.ManyToOne;

import static org.junit.Assert.assertEquals;

/**
 * @author Vlad Mihalcea
 */
public class DefaultNamingTest extends AbstractOracleXEIntegrationTest {

    @Override
    protected Class<?>[] entities() {
        return new Class<?>[] {
            PersonAddress.class,
            PersonLocation.class,
            Person.class,
        };
    }

    @Test
    public void test() {
        doInJPA(entityManager -> {
            PersonAddress address = new PersonAddress();
            address.id = 1L;

            PersonLocation location = new PersonLocation();
            location.id = 1L;

            Person person = new Person();
            person.personId = 1L;
            person.addressIsAVeryLongColumnThatExceedsThirtyCharacters = address;
            person.location = location;

            entityManager.persist(address);
            entityManager.persist(location);
            entityManager.persist(person);
        });

        doInJPA(entityManager -> {
            Person person = entityManager.find(Person.class, 1L);
            assertEquals(Long.valueOf(1L), person.addressIsAVeryLongColumnThatExceedsThirtyCharacters.id);
            assertEquals(Long.valueOf(1L), person.location.id);
        });
    }

    @Entity(name = "PersonAddress")
    public static class PersonAddress {

        @Id
        private Long id;
    }

    @Entity(name = "PersonLocation")
    public static class PersonLocation {

        @Id
        private Long id;
    }

    @Entity(name = "Person")
    public static class Person  {

        @Id
        private Long personId;

        @ManyToOne
        private PersonAddress addressIsAVeryLongColumnThatExceedsThirtyCharacters;

        @ManyToOne
        private PersonLocation location;
    }
}
