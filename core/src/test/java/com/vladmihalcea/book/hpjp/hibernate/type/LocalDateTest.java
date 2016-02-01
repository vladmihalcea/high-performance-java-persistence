package com.vladmihalcea.book.hpjp.hibernate.type;

import com.vladmihalcea.book.hpjp.util.AbstractTest;
import org.junit.Test;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.validation.constraints.NotNull;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;

import static org.junit.Assert.assertEquals;

/**
 * <code>LocalDateTest</code> - LocalDate Test
 *
 * @author Vlad Mihalcea
 */
public class LocalDateTest extends AbstractTest {

    @Override
    protected Class<?>[] entities() {
        return new Class<?>[] {
            LocalDateEvent.class,
            OffsetDateTimeEvent.class
        };
    }

    @Test
    public void testLocalDateEvent() {
        doInJPA(entityManager -> {
            LocalDateEvent event = new LocalDateEvent();
            event.id = 1L;
            event.startDate = LocalDate.of(1, 1, 1);
            entityManager.persist(event);
        });

        doInJPA(entityManager -> {
            LocalDateEvent event = entityManager.find(LocalDateEvent.class, 1L);
            try {
                assertEquals(LocalDate.of(1, 1, 1), event.startDate);
            } catch (Throwable e) {
                e.printStackTrace();
            }
        });
    }

    @Test
    public void testOffsetDateTimeEvent() {
        doInJPA(entityManager -> {
            OffsetDateTimeEvent event = new OffsetDateTimeEvent();
            event.id = 1L;
            event.startDate = OffsetDateTime.of(1, 1, 1, 0, 0, 0, 0, ZoneOffset.UTC);
            entityManager.persist(event);
        });

        doInJPA(entityManager -> {
            OffsetDateTimeEvent event = entityManager.find(OffsetDateTimeEvent.class, 1L);
            try {
                assertEquals(OffsetDateTime.of(1, 1, 1, 0, 0, 0, 0, ZoneOffset.UTC), event.startDate);
            } catch (Throwable e) {
                e.printStackTrace();
            }
        });
    }

    @Entity(name = "LocalDateEvent")
    public static class LocalDateEvent {

        @Id
        private Long id;

        @NotNull
        @Column(name = "START_DATE", nullable = false)
        private LocalDate startDate;
    }

    @Entity(name = "OffsetDateTimeEvent")
    public static class OffsetDateTimeEvent {

        @Id
        private Long id;

        @NotNull
        @Column(name = "START_DATE", nullable = false)
        private OffsetDateTime startDate;
    }
}
