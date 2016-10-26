package com.vladmihalcea.book.hpjp.hibernate.type;

import com.vladmihalcea.book.hpjp.util.AbstractMySQLIntegrationTest;
import org.hibernate.annotations.Type;
import org.junit.Test;

import javax.persistence.*;
import java.util.Arrays;
import java.util.concurrent.atomic.AtomicReference;

/**
 * @author Vlad Mihalcea
 */
public class MySQLTextStringTest extends AbstractMySQLIntegrationTest {

    @Override
    protected Class<?>[] entities() {
        return new Class<?>[] {
            Event.class
        };
    }
    @Test
    public void test() {
        final AtomicReference<Event> eventHolder = new AtomicReference<>();
        doInJPA(entityManager -> {
            entityManager.persist(new Event());
            Event event = new Event();
            char[] chars = new char[1000];
            Arrays.fill(chars, 'a');
            event.message = new String(chars);
            entityManager.persist(event);
            eventHolder.set(event);
        });
    }

    @Entity(name = "Event")
    @Table(name = "event")
    public static class Event {

        @Id
        @GeneratedValue
        private Long id;

        @Column(name="my_field", columnDefinition="text")
        @Type(type = "text")
        private String message;
    }
}
