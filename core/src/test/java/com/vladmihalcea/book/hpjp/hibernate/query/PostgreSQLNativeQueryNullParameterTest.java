package com.vladmihalcea.book.hpjp.hibernate.query;

import com.vladmihalcea.book.hpjp.util.AbstractPostgreSQLIntegrationTest;
import jakarta.persistence.*;
import org.hibernate.query.NativeQuery;
import org.junit.Test;

import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.List;

/**
 * @author Vlad Mihalcea
 */
public class PostgreSQLNativeQueryNullParameterTest extends AbstractPostgreSQLIntegrationTest {

    @Override
    protected Class<?>[] entities() {
        return new Class<?>[] {
            Event.class
        };
    }

    @Test
    public void test() {
        doInJPA(entityManager -> {
            Event firstPartRelease = new Event();
            firstPartRelease.setName("High-Performance Java Persistence Part I");
            firstPartRelease.setCreatedOn(
                Timestamp.from(LocalDateTime.of(2015, 11, 2, 9, 0, 0).toInstant(ZoneOffset.UTC))
            );
            entityManager.persist(firstPartRelease);
        });

        //Explicit cast needed as a workaround for HHH-13155
        doInJPA(entityManager -> {
            List<Event> events = entityManager
            .createNativeQuery(
                "SELECT * " +
                "FROM Event " +
                "WHERE (:name is null or name = :name)", Event.class)
            .unwrap(NativeQuery.class)
            .setParameter("name", null, String.class)
            .getResultList();
        });
    }

    @Entity(name = "Event")
    @Table(name = "event")
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
