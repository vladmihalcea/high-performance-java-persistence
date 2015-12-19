package com.vladmihalcea.book.hpjp.hibernate.collection;

import com.vladmihalcea.book.hpjp.hibernate.collection.type.CommaDelimitedStringsType;
import com.vladmihalcea.book.hpjp.util.AbstractTest;
import org.hibernate.annotations.Type;
import org.junit.Test;

import javax.persistence.Entity;
import javax.persistence.Id;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * <code>BasicTypeCollectionTest</code> - Basic Type Collection Test
 *
 * @author Vlad Mihalcea
 */
public class BasicTypeCollectionTest extends AbstractTest {

    @Override
    protected Class<?>[] entities() {
        return new Class<?>[] {
            Person.class
        };
    }

    @Test
    public void testLifecycle() {
        doInHibernate(session -> {
            Person person = new Person();
            person.id = 1L;
            person.phones.add("027-123-4567");
            person.phones.add("028-234-9876");
            session.persist(person);
        });
        doInHibernate(session -> {
            Person person = session.get(Person.class, 1L);
            LOGGER.info("Remove one element");
            person.getPhones().remove(0);
        });
    }

    @Entity(name = "Person")
    public static class Person  {

        @Id
        private Long id;

        @Type(type = "comma_delimited_strings")
        private List<String> phones = new ArrayList<>();

        public List<String> getPhones() {
            return phones;
        }
    }

    @Override
    protected boolean nativeHibernateSessionFactoryBootstrap() {
        return true;
    }

    @Override
    protected List<org.hibernate.type.Type> additionalTypes() {
        return Collections.singletonList(new CommaDelimitedStringsType());
    }
}
