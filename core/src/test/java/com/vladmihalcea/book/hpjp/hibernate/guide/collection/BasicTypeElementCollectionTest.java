package com.vladmihalcea.book.hpjp.hibernate.guide.collection;

import com.vladmihalcea.book.hpjp.util.AbstractTest;
import org.junit.Test;

import javax.persistence.*;
import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.assertEquals;

/**
 * <code>BasicTypeElementCollectionTest</code> - Basic Type Element Collection Test
 *
 * @author Vlad Mihalcea
 */
public class BasicTypeElementCollectionTest extends AbstractTest {

    @Override
    protected Class<?>[] entities() {
        return new Class<?>[] {
            Person.class
        };
    }

    @Override
    public void init() {
        super.init();
        doInJPA(entityManager -> {
            Person person = new Person();
            person.id = 1L;
            person.phones.add("027-123-4567");
            person.phones.add("028-234-9876");
            entityManager.persist(person);
        });
    }

    @Test
    public void testProxies() {
        doInJPA(entityManager -> {
            Person person = entityManager.find(Person.class, 1L);
            assertEquals(2, person.getPhones().size());
            try {
                ArrayList<String> phones = (ArrayList<String>) person.getPhones();
            } catch (Exception expected) {
                LOGGER.error("Failure", expected);
            }
        });
    }

    @Test
    public void testLifecycle() {
        doInJPA(entityManager -> {
            Person person = entityManager.find(Person.class, 1L);
            LOGGER.info("Clear element collection and add element");
            person.getPhones().clear();
            person.getPhones().add("123-456-7890");
            person.getPhones().add("456-000-1234");
        });
        doInJPA(entityManager -> {
            Person person = entityManager.find(Person.class, 1L);
            LOGGER.info("Remove one element");
            person.getPhones().remove(0);
        });
    }

    @Entity(name = "Person")
    public static class Person  {

        @Id
        private Long id;

        @ElementCollection
        private List<String> phones = new ArrayList<>();

        public List<String> getPhones() {
            return phones;
        }
    }
}
