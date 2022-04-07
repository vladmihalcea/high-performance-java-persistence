package com.vladmihalcea.book.hpjp.hibernate.mapping.calculated;

import com.vladmihalcea.book.hpjp.util.AbstractPostgreSQLIntegrationTest;
import org.hibernate.annotations.Formula;
import org.junit.Test;

import jakarta.persistence.*;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.Date;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

/**
 * @author Vlad Mihalcea
 */
public class FormulaCurrentDateTest extends AbstractPostgreSQLIntegrationTest {

    @Override
    protected Class<?>[] entities() {
        return new Class<?>[] {
            Event.class,
        };
    }

    @Test
    public void test() {
        doInJPA(entityManager -> {
            Event event = new Event();
            event.setId(1L);

            entityManager.persist(event);
            entityManager.flush();

            entityManager.refresh(event);

            assertNotNull(event.getCreatedOn());
        });
    }

    @Entity(name = "Event")
    @Table(name = "event")
    public static class Event {

        @Id
        private Long id;

        @Formula("(SELECT current_date)")
        @Temporal(TemporalType.TIMESTAMP)
        private Date createdOn;

        public Long getId() {
            return id;
        }

        public void setId(Long id) {
            this.id = id;
        }

        public Date getCreatedOn() {
            return createdOn;
        }

        public void setCreatedOn(Date createdOn) {
            this.createdOn = createdOn;
        }
    }
}
