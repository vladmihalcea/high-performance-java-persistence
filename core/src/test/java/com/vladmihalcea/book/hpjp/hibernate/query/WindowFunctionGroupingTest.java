package com.vladmihalcea.book.hpjp.hibernate.query;

import com.vladmihalcea.book.hpjp.util.AbstractPostgreSQLIntegrationTest;
import org.junit.Test;

import javax.persistence.*;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.Date;
import java.util.List;

import static org.junit.Assert.assertEquals;

/**
 * @author Vlad Mihalcea
 */
public class WindowFunctionGroupingTest extends AbstractPostgreSQLIntegrationTest {

    @Override
    protected Class<?>[] entities() {
        return new Class<?>[] {
                DataEvent.class,
        };
    }

    @Test
    public void test() {
        doInJPA(entityManager -> {
            DataEvent event1 = new DataEvent();
            event1.setCategory("Living room");
            event1.setKpi("Temperature");
            event1.setValue(21.5d);
            event1.setCreatedOn(Timestamp.valueOf(LocalDateTime.now().minusDays(1)));

            DataEvent event2 = new DataEvent();
            event2.setCategory("Living room");
            event2.setKpi("Temperature");
            event2.setValue(22.5d);
            event2.setCreatedOn(Timestamp.valueOf(LocalDateTime.now().minusDays(2)));

            DataEvent event3 = new DataEvent();
            event3.setCategory("Living room");
            event3.setKpi("Temperature");
            event3.setValue(20.5d);
            event3.setCreatedOn(Timestamp.valueOf(LocalDateTime.now().minusDays(2)));

            DataEvent event4 = new DataEvent();
            event4.setCategory("Bedroom");
            event4.setKpi("Temperature");
            event4.setValue(23.5d);
            event4.setCreatedOn(Timestamp.valueOf(LocalDateTime.now().minusDays(1)));

            DataEvent event5 = new DataEvent();
            event5.setCategory("Bedroom");
            event5.setKpi("Pressure");
            event5.setValue(750.5d);
            event5.setCreatedOn(Timestamp.valueOf(LocalDateTime.now().minusDays(1)));

            DataEvent event6 = new DataEvent();
            event6.setCategory("Living room");
            event6.setKpi("Temperature");
            event6.setValue(22.5d);
            event6.setCreatedOn(Timestamp.valueOf(LocalDateTime.now().minusDays(1)));

            entityManager.persist(event1);
            entityManager.persist(event2);
            entityManager.persist(event3);
            entityManager.persist(event5);
            entityManager.persist(event5);
            entityManager.persist(event6);
        });

        doInJPA(entityManager -> {
            List<Object[]> values = entityManager.createNativeQuery(
                    "select de3.category, de3.kpi, de3.value " +
                    "from ( " +
                    "    select de2.category, de2.kpi, de2.value, de2.createdon, max(de2.createdon) over (partition by de2.category, de2.kpi) as max_createdon " +
                    "    from ( " +
                    "        select  " +
                    "            de1.category, de1.kpi, de1.value, de1.createdon " +
                    "        from dataevent de1 " +
                    "    ) de2 " +
                    ") de3 " +
                    "where de3.createdon = max_createdon")
            .getResultList();
            assertEquals(2, values.size());
        });
    }

    @Entity(name = "DataEvent")
    public static class DataEvent  {

        @Id
        @GeneratedValue
        private Long id;

        private String category;

        private String kpi;

        @Temporal(TemporalType.TIMESTAMP)
        private Date createdOn;

        private Double value;

        public Long getId() {
            return id;
        }

        public void setId(Long id) {
            this.id = id;
        }

        public String getCategory() {
            return category;
        }

        public void setCategory(String category) {
            this.category = category;
        }

        public String getKpi() {
            return kpi;
        }

        public void setKpi(String kpi) {
            this.kpi = kpi;
        }

        public Date getCreatedOn() {
            return createdOn;
        }

        public void setCreatedOn(Date createdOn) {
            this.createdOn = createdOn;
        }

        public Double getValue() {
            return value;
        }

        public void setValue(Double value) {
            this.value = value;
        }
    }
}
