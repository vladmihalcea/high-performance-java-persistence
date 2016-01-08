package com.vladmihalcea.guide.mapping;

import com.vladmihalcea.book.hpjp.util.AbstractTest;
import org.junit.Test;

import javax.persistence.*;
import java.time.LocalDateTime;

/**
 * <code>LocalDateTimeWithTemporalTimeTest</code> - LocalDateTime Test
 *
 * @author Vlad Mihalcea
 */
public class LocalDateTimeWithTemporalTimeTest extends AbstractTest {

    @Override
    protected Class<?>[] entities() {
        return new Class<?>[] {
            DateEvent.class
        };
    }

    @Test
    public void testLifecycle() {
        doInJPA(entityManager -> {
            DateEvent dateEvent = new DateEvent(LocalDateTime.now());
            entityManager.persist(dateEvent);
        });
    }

    @Entity(name = "DateEvent")
    public static class DateEvent  {

        @Id
        @GeneratedValue
        private Long id;

        //throws org.hibernate.AnnotationException: @Temporal should only be set on a java.util.Date or java.util.Calendar property
        //@Temporal(TemporalType.TIME)
        private LocalDateTime timestamp;

        public DateEvent() {}

        public DateEvent(LocalDateTime timestamp) {
            this.timestamp = timestamp;
        }

        public Long getId() {
            return id;
        }

        public LocalDateTime getTimestamp() {
            return timestamp;
        }
    }
}
