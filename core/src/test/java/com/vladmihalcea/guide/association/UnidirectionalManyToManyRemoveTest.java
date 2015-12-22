package com.vladmihalcea.guide.association;

import com.vladmihalcea.book.hpjp.util.AbstractTest;
import org.junit.Test;

import javax.persistence.*;
import java.util.ArrayList;
import java.util.List;

/**
 * <code>UnidirectionalManyToManyTest</code> - Unidirectional @ManyToMany Test
 *
 * @author Vlad Mihalcea
 */
public class UnidirectionalManyToManyRemoveTest extends AbstractTest {

    @Override
    protected Class<?>[] entities() {
        return new Class<?>[] {
            Person.class,
            Address.class,
        };
    }

    @Test
    public void testRemove() {
        try {
            final Long personId = doInJPA(entityManager -> {
                Person person1 = new Person();
                Person person2 = new Person();

                Address address1 = new Address("12th Avenue", "12A");
                Address address2 = new Address("18th Avenue", "18B");

                person1.getAddresses().add(address1);
                person1.getAddresses().add(address2);

                person2.getAddresses().add(address1);

                entityManager.persist(person1);
                entityManager.persist(person2);

                return person1.id;
            });
            doInJPA(entityManager -> {

                Person person1 = entityManager.find(Person.class, personId);
                entityManager.remove(person1);
            });
        } catch (Exception expected) {
           LOGGER.error("Expected", expected);
        }
    }

    @Entity(name = "Person")
    public static class Person  {

        @Id
        @GeneratedValue
        private Long id;

        public Person() {}

        @ManyToMany(cascade = { CascadeType.ALL} )
        private List<Address> addresses = new ArrayList<>();

        public List<Address> getAddresses() {
            return addresses;
        }
    }

    @Entity(name = "Address")
    public static class Address  {

        @Id
        @GeneratedValue
        private Long id;

        private String street;

        private String number;

        public Address() {}

        public Address(String street, String number) {
            this.street = street;
            this.number = number;
        }

        public Long getId() {
            return id;
        }

        public String getStreet() {
            return street;
        }

        public String getNumber() {
            return number;
        }
    }
}
