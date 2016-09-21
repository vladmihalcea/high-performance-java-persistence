package com.vladmihalcea.book.hpjp.hibernate.fetching;

import com.vladmihalcea.book.hpjp.util.AbstractTest;
import org.junit.Test;

import javax.persistence.*;
import javax.persistence.criteria.*;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * @author Vlad Mihalcea
 */
public class CriteriaAPIMemberOfTest extends AbstractTest {

    @Override
    protected Class<?>[] entities() {
        return new Class[] {
            CalendarEvent.class
        };
    }


    @Override
    public void init() {
        super.init();
        doInJPA(entityManager -> {
            CalendarEvent event = new CalendarEvent();
            event.mailingCodes.add(1);
            event.mailingCodes.add(2);
            event.mailingCodes.add(3);
            event.mailingCodes.add(4);
            entityManager.persist(event);
        });
    }

    @Test
    public void testFind() {
        doInJPA(entityManager -> {
            CriteriaBuilder builder = entityManager.getCriteriaBuilder();
            CriteriaQuery<CalendarEvent> criteria = builder.createQuery(CalendarEvent.class);
            Root<CalendarEvent> root = criteria.from(CalendarEvent.class);

            List<MailingCode> mailingCodes = Arrays.asList(
                new MailingCode(1),
                new MailingCode(2),
                new MailingCode(3)
            );

            Expression<List<Integer>> mailingCodesPath = root.get("mailingCodes");

            Predicate predicate = builder.conjunction();

            for(MailingCode mailingCode: mailingCodes){
                predicate = builder.and(predicate, builder.isMember(mailingCode.getId(), mailingCodesPath));
            }

            criteria.where(predicate);
            List<CalendarEvent> events = entityManager.createQuery(criteria).getResultList();

            return events;
        });
    }

    @Entity(name = "CalendarEvent")
    @Table
    public static class CalendarEvent implements Serializable {

        @Id
        @GeneratedValue(strategy = GenerationType.AUTO)
        private Integer id;

        @ElementCollection
        private final List<Integer> mailingCodes = new ArrayList<>();

    }

    public static class MailingCode {
        private Integer id;

        public MailingCode(Integer id) {
            this.id = id;
        }

        public Integer getId() {
            return id;
        }
    }
}
