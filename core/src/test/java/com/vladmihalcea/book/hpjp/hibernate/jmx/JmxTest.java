package com.vladmihalcea.book.hpjp.hibernate.jmx;

import com.vladmihalcea.book.hpjp.util.AbstractTest;
import org.hibernate.cfg.AvailableSettings;
import org.junit.Ignore;
import org.junit.Test;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import java.util.ArrayList;
import java.util.List;
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
        properties.put("hibernate.jmx.usePlatformServer", Boolean.TRUE.toString());
        properties.put("hibernate.cache.use_second_level_cache", Boolean.TRUE.toString());
        properties.put("hibernate.cache.region.factory_class", "jcache");
    }

    private int seconds = 120;

    @Test
    @Ignore
    public void test() {
        long startNanos = System.nanoTime();

        AtomicLong idHolder = new AtomicLong();
        List<Long> ids = new ArrayList<>();

        doInJPA(entityManager -> {
            while (TimeUnit.NANOSECONDS.toSeconds(System.nanoTime() - startNanos) < seconds) {
                sleep(1000);
                if (Math.random() < 0.25) {
                    Long id = idHolder.incrementAndGet();
                    ids.add(id);
                    entityManager.persist(
                        new Person()
                            .setId(id)
                            .setFirstName(String.format("First Name - %d", id))
                            .setLastName(String.format("Last Name - %d", id))
                    );
                } else {
                    List<Person> persons = entityManager.createQuery("""
                        select p
                        from Person p
                        where p.id in :ids
                        """, Person.class)
                    .setParameter("ids",ids)
                    .getResultList();
                    LOGGER.info("Person count: {}", persons.size());
                }
            }
        });
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

        public Person setId(Long id) {
            this.id = id;
            return this;
        }

        public String getFirstName() {
            return firstName;
        }

        public Person setFirstName(String firstName) {
            this.firstName = firstName;
            return this;
        }

        public String getLastName() {
            return lastName;
        }

        public Person setLastName(String lastName) {
            this.lastName = lastName;
            return this;
        }
    }
}
