package com.vladmihalcea.guide.collection;

import com.vladmihalcea.book.hpjp.util.AbstractTest;
import org.junit.Test;

import javax.persistence.Entity;
import javax.persistence.Id;

/**
 * <code>UnidirectionalBag</code> - Unidirectional Bag Test
 *
 * @author Vlad Mihalcea
 */
public class UnidirectionalArrayTest extends AbstractTest {

    @Override
    protected Class<?>[] entities() {
        return new Class<?>[] {
            Person.class
        };
    }

    @Test
    public void testLifecycle() {
        doInJPA(entityManager -> {
            Person person = new Person(1L);
            String[] phones = new String[2];
            phones[0] = "028-234-9876";
            phones[1] = "072-122-9876";
            person.setPhones(phones);
            entityManager.persist(person);
        });
        doInJPA(entityManager -> {
            Person person = entityManager.find(Person.class, 1L);
            String[] phones = new String[1];
            phones[0] = "072-122-9876";
            person.setPhones(phones);
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

        private String[] phones;

        public String[] getPhones() {
            return phones;
        }

        public void setPhones(String[] phones) {
            this.phones = phones;
        }
    }
}
