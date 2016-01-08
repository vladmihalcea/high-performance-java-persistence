package com.vladmihalcea.guide.mapping;

import com.vladmihalcea.book.hpjp.util.AbstractTest;
import org.junit.Test;

import javax.persistence.*;
import java.util.Calendar;
import java.util.GregorianCalendar;

import static org.junit.Assert.assertEquals;

/**
 * <code>CalendarWithTemporalTimestampTest</code> - Calendar Test
 *
 * @author Vlad Mihalcea
 */
public class CalendarWithTemporalTimestampTest extends AbstractTest {

    @Override
    protected Class<?>[] entities() {
        return new Class<?>[] {
            DateEvent.class
        };
    }

    @Test
    public void testLifecycle() {
        final Calendar calendar = new GregorianCalendar();
        doInJPA(entityManager -> {
            entityManager.persist(new DateEvent(calendar) );
        });
        doInJPA(entityManager -> {
            DateEvent dateEvent = entityManager.createQuery("from DateEvent", DateEvent.class).getSingleResult();
            assertEquals(calendar, dateEvent.getTimestamp());
        });
    }

    @Entity(name = "DateEvent")
    public static class DateEvent  {

        @Id
        @GeneratedValue
        private Long id;

        @Temporal(TemporalType.TIMESTAMP)
        private Calendar timestamp;

        public DateEvent() {}

        public DateEvent(Calendar timestamp) {
            this.timestamp = timestamp;
        }

        public Long getId() {
            return id;
        }

        public Calendar getTimestamp() {
            return timestamp;
        }
    }
}
