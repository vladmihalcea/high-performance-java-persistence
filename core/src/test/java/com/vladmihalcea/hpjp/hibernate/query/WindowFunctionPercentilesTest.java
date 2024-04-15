package com.vladmihalcea.hpjp.hibernate.query;

import com.vladmihalcea.hpjp.util.AbstractTest;
import com.vladmihalcea.hpjp.util.providers.Database;
import jakarta.persistence.*;
import org.junit.Test;

import java.math.BigDecimal;
import java.sql.Timestamp;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Date;
import java.util.List;
import java.util.Properties;

import static org.hibernate.cfg.SchemaToolingSettings.JAKARTA_HBM2DDL_CREATE_SCRIPT_SOURCE;
import static org.hibernate.cfg.SchemaToolingSettings.JAKARTA_HBM2DDL_LOAD_SCRIPT_SOURCE;
import static org.junit.Assert.assertEquals;

/**
 * @author Vlad Mihalcea
 */
public class WindowFunctionPercentilesTest extends AbstractTest {

    @Override
    protected Class<?>[] entities() {
        return new Class<?>[] {
            SP500.class,
        };
    }

    @Override
    protected Database database() {
        return Database.POSTGRESQL;
    }

    @Override
    protected void additionalProperties(Properties properties) {
        properties.put(JAKARTA_HBM2DDL_LOAD_SCRIPT_SOURCE, "data/sp500.sql");
    }

    @Test
    public void test() {
        doInJPA(entityManager -> {
            List<Tuple> prices = entityManager.createNativeQuery("""    
                SELECT
                   PERCENTILE_CONT(0.50) WITHIN GROUP(ORDER BY close_price) AS median,
                   PERCENTILE_CONT(0.75) WITHIN GROUP(ORDER BY close_price) AS p75,
                   PERCENTILE_CONT(0.95) WITHIN GROUP(ORDER BY close_price) AS p95,
                   PERCENTILE_CONT(0.99) WITHIN GROUP(ORDER BY close_price) AS p99
                FROM sp500
                """, Tuple.class)
            .getResultList();

            assertEquals(1, prices.size());
        });

        doInJPA(entityManager -> {
            List<Tuple> prices = entityManager.createNativeQuery("""    
                SELECT
                   extract(year from price_date) AS year,
                   PERCENTILE_CONT(0.50) WITHIN GROUP(ORDER BY close_price) AS median,
                   PERCENTILE_CONT(0.75) WITHIN GROUP(ORDER BY close_price) AS p75,
                   PERCENTILE_CONT(0.95) WITHIN GROUP(ORDER BY close_price) AS p95,
                   PERCENTILE_CONT(0.99) WITHIN GROUP(ORDER BY close_price) AS p99
                FROM sp500
                GROUP BY extract(year from price_date)
                """, Tuple.class)
            .getResultList();

            assertEquals(5, prices.size());
        });
    }

    @Entity(name = "SP500")
    public static class SP500 {

        @Id
        @Column(name = "price_date")
        private LocalDate date;

        @Column(name = "close_price")
        private BigDecimal price;

        public LocalDate getDate() {
            return date;
        }

        public void setDate(LocalDate date) {
            this.date = date;
        }

        public BigDecimal getPrice() {
            return price;
        }

        public void setPrice(BigDecimal price) {
            this.price = price;
        }
    }
}
