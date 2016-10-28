package com.vladmihalcea.book.hpjp.hibernate.query;

import com.vladmihalcea.book.hpjp.util.AbstractPostgreSQLIntegrationTest;
import com.vladmihalcea.book.hpjp.util.AbstractTest;
import org.hibernate.cfg.AvailableSettings;
import org.junit.Test;

import javax.persistence.*;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Properties;

import static org.junit.Assert.assertEquals;

/**
 * @author Vlad Mihalcea
 */
public class NativeQueryWithCustomSchemaTest extends AbstractPostgreSQLIntegrationTest {

    @Override
    protected Class<?>[] entities() {
        return new Class<?>[] {
            Event.class
        };
    }

    @Override
    protected Properties properties() {
        Properties properties = super.properties();
        properties.setProperty(AvailableSettings.DEFAULT_SCHEMA, "forum");
        return properties;
    }

    @Test
    public void test() {
        doInJPA(entityManager -> {
            Event firstPartRelease = new Event();
            firstPartRelease.setName("High-Performance Java Persistence Part I");
            firstPartRelease.setCreatedOn(Timestamp.from(LocalDateTime.of(2015, 11, 2, 9, 0, 0).toInstant(ZoneOffset.UTC)));
            entityManager.persist(firstPartRelease);

            Event finalRelease = new Event();
            finalRelease.setName("High-Performance Java Persistence Part I");
            finalRelease.setCreatedOn(Timestamp.from(LocalDateTime.of(2016, 8, 25, 9, 0, 0).toInstant(ZoneOffset.UTC)));
            entityManager.persist(finalRelease);
        });

        doInJPA(entityManager -> {
            List<Event> events = entityManager.createQuery(
                "select e " +
                "from Event e " +
                "where e.createdOn > :timestamp", Event.class)
            .setParameter("timestamp", Timestamp.valueOf(LocalDateTime.now().minusMonths(1)))
            .getResultList();
        });

        doInJPA(entityManager -> {
            List<Event> events = entityManager.createNamedQuery("past_30_days_events").getResultList();
        });
    }

    @Entity(name = "Event")
    @Table(name = "event")
    /*@NamedNativeQuery(
        name = "past_30_days_events",
        query =
            "select * " +
            "from event " +
            "where age(created_on) > '30 days'",
        resultClass = Event.class
    )*/
    @NamedNativeQuery(
        name = "past_30_days_events",
        query =
            "select * " +
            "from {h-schema}event " +
            "where age(created_on) > '30 days'",
        resultClass = Event.class
    )
    public static class Event {

        @Id
        @GeneratedValue
        private long id;

        private String name;

        @Column(name = "created_on")
        private Timestamp createdOn;

        public long getId() {
            return id;
        }

        public void setId(long id) {
            this.id = id;
        }

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public Timestamp getCreatedOn() {
            return createdOn;
        }

        public void setCreatedOn(Timestamp createdOn) {
            this.createdOn = createdOn;
        }
    }
}
