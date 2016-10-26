package com.vladmihalcea.book.hpjp.hibernate.jmx;

import com.vladmihalcea.book.hpjp.util.AbstractTest;
import org.hibernate.cfg.AvailableSettings;
import org.junit.Test;

import javax.persistence.Entity;
import javax.persistence.Id;
import java.util.Properties;

/**
 * @author Vlad Mihalcea
 */
public class JmxTest extends AbstractTest {

    @Override
    protected Class<?>[] entities() {
        return new Class<?>[] {
            Person.class,
        };
    }

    @Override
    protected Properties properties() {
        Properties properties = super.properties();
        properties.put(AvailableSettings.JMX_ENABLED, Boolean.TRUE.toString());
        return properties;
    }

    @Test
    public void test() {
        doInJPA(entityManager -> {
            Person vlad = new Person();
            vlad.id = 1L;
            entityManager.persist(vlad);
        });
    }

    @Entity(name = "Person")
    public static class Person  {

        @Id
        private Long id;

        private String firstName;

        private String lastName;
    }
}
