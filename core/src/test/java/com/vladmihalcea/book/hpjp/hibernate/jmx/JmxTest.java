package com.vladmihalcea.book.hpjp.hibernate.jmx;

import com.vladmihalcea.book.hpjp.util.AbstractTest;
import org.hibernate.cfg.AvailableSettings;
import org.junit.Ignore;
import org.junit.Test;

import javax.persistence.Entity;
import javax.persistence.Id;
import java.util.Properties;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

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
    protected void additionalProperties(Properties properties) {
        properties.put(AvailableSettings.GENERATE_STATISTICS, Boolean.TRUE.toString());
        properties.put("hibernate.jmx.enabled", Boolean.TRUE.toString());
        properties.put("hibernate.cache.use_second_level_cache", Boolean.TRUE.toString());
        properties.put("hibernate.cache.region.factory_class", "ehcache");
    }

    private int seconds = 120;

    @Test
    @Ignore
    public void test() {
        long startNanos = System.nanoTime();

        AtomicLong id = new AtomicLong();

        while (TimeUnit.NANOSECONDS.toSeconds(System.nanoTime() - startNanos) < seconds) {
            doInJPA(entityManager -> {
                if (Math.random() < 0.1) {
                    Person person = new Person();
                    person.setId(id.incrementAndGet());

                    entityManager.persist(person);
                }
            });
        }
    }

    @Entity(name = "Person")
    public static class Person  {

        @Id
        private Long id;

        private String firstName;

        private String lastName;

        public Long getId() {
            return id;
        }

        public void setId(Long id) {
            this.id = id;
        }

        public String getFirstName() {
            return firstName;
        }

        public void setFirstName(String firstName) {
            this.firstName = firstName;
        }

        public String getLastName() {
            return lastName;
        }

        public void setLastName(String lastName) {
            this.lastName = lastName;
        }
    }
}
