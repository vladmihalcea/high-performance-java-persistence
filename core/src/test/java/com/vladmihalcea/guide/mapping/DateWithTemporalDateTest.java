package com.vladmihalcea.guide.mapping;

import com.vladmihalcea.book.hpjp.util.AbstractTest;
import org.junit.Test;

import javax.persistence.*;
import java.util.Date;

/**
 * <code>DateWithTemporalDateTest</code> - Date Test
 *
 * @author Vlad Mihalcea
 */
public class DateWithTemporalDateTest extends AbstractTest {

    @Override
    protected Class<?>[] entities() {
        return new Class<?>[] {
            DateEvent.class
        };
    }

    @Test
    public void testLifecycle() {
        doInJPA(entityManager -> {
            DateEvent dateEvent = new DateEvent(new Date());
            entityManager.persist(dateEvent);
        });
    }

    @Entity(name = "DateEvent")
    public static class DateEvent  {

        @Id
        @GeneratedValue
        private Long id;

        @Temporal(TemporalType.DATE)
        private Date timestamp;

        public DateEvent() {}

        public DateEvent(Date timestamp) {
            this.timestamp = timestamp;
        }

        public Long getId() {
            return id;
        }

        public Date getTimestamp() {
            return timestamp;
        }
    }
}
