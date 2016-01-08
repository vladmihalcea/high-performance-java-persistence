package com.vladmihalcea.guide.mapping.converter;

import com.vladmihalcea.book.hpjp.util.AbstractTest;
import org.junit.Test;

import javax.persistence.*;
import java.time.Period;

import static org.junit.Assert.assertEquals;

/**
 * <code>PeriodStringTest</code> - Period Test
 *
 * @author Vlad Mihalcea
 */
public class PeriodStringTest extends AbstractTest {

    private Period period = Period.ofYears(1).plusMonths(2).plusDays(3);

    @Override
    protected Class<?>[] entities() {
        return new Class<?>[] {
                Event.class
        };
    }

    @Test
    public void testLifecycle() {
        doInJPA(entityManager -> {
            Event event = new Event(period);
            entityManager.persist(event);
        });
        doInJPA(entityManager -> {
            Event event = entityManager.createQuery("from Event", Event.class).getSingleResult();
            assertEquals(period, event.getSpan());
        });
    }

    @Entity(name = "Event")
    public static class Event  {

        @Id
        @GeneratedValue
        private Long id;

        @Convert(converter = PeriodStringConverter.class)
        @Column(columnDefinition = "")
        private Period span;

        public Event() {}

        public Event(Period span) {
            this.span = span;
        }

        public Long getId() {
            return id;
        }

        public Period getSpan() {
            return span;
        }
    }
}
