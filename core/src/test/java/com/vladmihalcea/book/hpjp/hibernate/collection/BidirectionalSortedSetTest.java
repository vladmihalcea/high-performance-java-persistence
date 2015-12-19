package com.vladmihalcea.book.hpjp.hibernate.collection;

import com.vladmihalcea.book.hpjp.util.AbstractTest;
import org.hibernate.annotations.NaturalId;
import org.hibernate.annotations.SortNatural;
import org.junit.Test;

import javax.persistence.*;
import java.util.*;

import static org.junit.Assert.assertEquals;

/**
 * <code>BidirectionalSortedSetTest</code> - Bidirectional SortedSet Test
 *
 * @author Vlad Mihalcea
 */
public class BidirectionalSortedSetTest extends AbstractTest {

    @Override
    protected Class<?>[] entities() {
        return new Class<?>[] {
            Person.class,
                Phone.class,
        };
    }

    @Test
    public void testLifecycle() {
        doInJPA(entityManager -> {
            Person person = new Person(1L);
            entityManager.persist(person);
            person.addPhone(new Phone(1L, "landline", "028-234-9876"));
            person.addPhone(new Phone(2L, "mobile", "072-122-9876"));
        });
        doInJPA(entityManager -> {
            Person person = entityManager.find(Person.class, 1L);
            Set<Phone> phones = person.getPhones();
            assertEquals(2, phones.size());
            phones.stream().forEach(phone -> LOGGER.info("Phone number {}", phone.getNumber()));
            person.removePhone(phones.iterator().next());
            assertEquals(1, phones.size());
        });
        doInJPA(entityManager -> {
            Person person = entityManager.find(Person.class, 1L);
            Set<Phone> phones = person.getPhones();
            assertEquals(1, phones.size());
        });
    }

    @Entity(name = "Person")
    public static class Person  {

        @Id
        private Long id;

        public Person() {}

        public Person(Long id) {
            this.id = id;
        }

        @OneToMany(mappedBy = "person", cascade = CascadeType.ALL)
        @SortNatural
        private SortedSet<Phone> phones = new TreeSet<>();

        public Set<Phone> getPhones() {
            return phones;
        }

        public void addPhone(Phone phone) {
            phones.add(phone);
            phone.setPerson(this);
        }

        public void removePhone(Phone phone) {
            phones.remove(phone);
            phone.setPerson(null);
        }
    }

    @Entity(name = "Phone")
    public static class Phone implements Comparable<Phone> {

        @Id
        private Long id;

        private String type;

        @Column(unique = true)
        @NaturalId
        private String number;

        @ManyToOne
        private Person person;

        public Phone() {
        }

        public Phone(Long id, String type, String number) {
            this.id = id;
            this.type = type;
            this.number = number;
        }

        public Long getId() {
            return id;
        }

        public String getType() {
            return type;
        }

        public String getNumber() {
            return number;
        }

        public Person getPerson() {
            return person;
        }

        public void setPerson(Person person) {
            this.person = person;
        }

        @Override
        public int compareTo(Phone o) {
            return number.compareTo(o.getNumber());
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            Phone phone = (Phone) o;
            return Objects.equals(number, phone.number);
        }

        @Override
        public int hashCode() {
            return Objects.hash(number);
        }
    }
}
