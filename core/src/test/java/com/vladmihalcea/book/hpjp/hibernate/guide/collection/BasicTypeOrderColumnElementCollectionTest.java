package com.vladmihalcea.book.hpjp.hibernate.guide.collection;

import com.vladmihalcea.book.hpjp.util.AbstractTest;
import org.junit.Test;

import javax.persistence.ElementCollection;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.OrderColumn;
import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.assertEquals;

/**
 * <code>BasicTypeElementCollectionTest</code> - Basic Type Element Collection Test
 *
 * @author Vlad Mihalcea
 */
public class BasicTypeOrderColumnElementCollectionTest extends AbstractTest {

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
            person.getPhones().add("123-456-7890");
            person.getPhones().add("456-000-1234");
            entityManager.persist(person);
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
        @OrderColumn(name = "order_id")
        private List<String> phones = new ArrayList<>();

        public List<String> getPhones() {
            return phones;
        }
    }
}
