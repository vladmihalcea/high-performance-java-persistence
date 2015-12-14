package com.vladmihalcea.book.hpjp.hibernate.collection;

import com.vladmihalcea.book.hpjp.util.AbstractTest;
import org.junit.Test;

import javax.persistence.ElementCollection;
import javax.persistence.Embeddable;
import javax.persistence.Entity;
import javax.persistence.Id;
import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.assertEquals;

/**
 * <code>EmbeddableTypeElementCollectionTest</code> - Embeddable Element Collection Test
 *
 * @author Vlad Mihalcea
 */
public class EmbeddableTypeElementCollectionTest extends AbstractTest {

    @Override
    protected Class<?>[] entities() {
        return new Class<?>[] {
            Person.class
        };
    }

    @Test
    public void testLifecycle() {
        doInJPA(entityManager -> {
            Person person = new Person();
            person.id = 1L;
            person.getPhones().add(new Phone("landline", "028-234-9876"));
            person.getPhones().add(new Phone("mobile", "072-122-9876"));
            entityManager.persist(person);
        });
    }

    @Entity(name = "Person")
    public static class Person  {

        @Id
        private Long id;

        @ElementCollection
        private List<Phone> phones = new ArrayList<>();

        public List<Phone> getPhones() {
            return phones;
        }
    }

    @Embeddable
    public static class Phone  {

        private String type;

        private String number;

        public Phone() {
        }

        public Phone(String type, String number) {
            this.type = type;
            this.number = number;
        }

        public String getType() {
            return type;
        }

        public String getNumber() {
            return number;
        }
    }
}
