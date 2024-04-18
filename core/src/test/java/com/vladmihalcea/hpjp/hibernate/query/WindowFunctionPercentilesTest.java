package com.vladmihalcea.hpjp.hibernate.query;

import com.vladmihalcea.hpjp.util.AbstractTest;
import com.vladmihalcea.hpjp.util.providers.Database;
import jakarta.persistence.*;
import org.junit.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Locale;
import java.util.Properties;

import static org.hibernate.cfg.SchemaToolingSettings.JAKARTA_HBM2DDL_LOAD_SCRIPT_SOURCE;
import static org.junit.Assert.assertEquals;

/**
 * @author Vlad Mihalcea
 */
public class WindowFunctionPercentilesTest extends AbstractTest {

    @Override
    protected Class<?>[] entities() {
        return new Class<?>[] {
            Quote.class,
        };
    }

    @Override
    protected Database database() {
        return Database.SQLSERVER;
    }

    @Override
    protected void additionalProperties(Properties properties) {
        properties.put(
            JAKARTA_HBM2DDL_LOAD_SCRIPT_SOURCE,
            database() == Database.ORACLE ? "data/oracle_quotes.sql" : "data/quotes.sql"
        );
    }

    @Test
    public void test() {
        if(!(database() == Database.ORACLE || database() == Database.POSTGRESQL)) {
            return;
        }
        doInJPA(entityManager -> {
            List<Tuple> prices = entityManager.createNativeQuery("""    
                SELECT
                    PERCENTILE_CONT(0.50) WITHIN GROUP(ORDER BY close_price) AS median,
                    PERCENTILE_CONT(0.75) WITHIN GROUP(ORDER BY close_price) AS p75,
                    PERCENTILE_CONT(0.95) WITHIN GROUP(ORDER BY close_price) AS p95,
                    PERCENTILE_CONT(0.99) WITHIN GROUP(ORDER BY close_price) AS p99
                FROM quotes
                WHERE
                    ticker = 'SPX' AND
                    quote_date BETWEEN DATE '2019-01-01' AND DATE '2023-12-31'
                """, Tuple.class)
            .getResultList();

            assertEquals(1, prices.size());
        });

        doInJPA(entityManager -> {
            List<Tuple> prices = entityManager.createNativeQuery("""    
                SELECT
                   extract(year from quote_date) AS year,
                   PERCENTILE_CONT(0.50) WITHIN GROUP(ORDER BY close_price) AS median,
                   PERCENTILE_CONT(0.75) WITHIN GROUP(ORDER BY close_price) AS p75,
                   PERCENTILE_CONT(0.95) WITHIN GROUP(ORDER BY close_price) AS p95,
                   PERCENTILE_CONT(0.99) WITHIN GROUP(ORDER BY close_price) AS p99
                FROM quotes
                WHERE
                    ticker = 'SPX' AND
                    quote_date BETWEEN DATE '2019-01-01' AND DATE '2023-12-31'
                GROUP BY extract(year from quote_date)
                ORDER BY year
                """, Tuple.class)
            .getResultList();

            assertEquals(5, prices.size());
        });
    }

    @Test
    public void testSQLServer() {
        if(!(database() == Database.SQLSERVER)) {
            return;
        }
        doInJPA(entityManager -> {
            List<Tuple> prices = entityManager.createNativeQuery("""    
                SELECT DISTINCT 
                    PERCENTILE_CONT(0.50) WITHIN GROUP(ORDER BY close_price) 
                        OVER (PARTITION BY ticker) AS median,
                    PERCENTILE_CONT(0.75) WITHIN GROUP(ORDER BY close_price) 
                        OVER (PARTITION BY ticker) AS p75,
                    PERCENTILE_CONT(0.95) WITHIN GROUP(ORDER BY close_price) 
                        OVER (PARTITION BY ticker) AS p95,
                    PERCENTILE_CONT(0.99) WITHIN GROUP(ORDER BY close_price) 
                        OVER (PARTITION BY ticker) AS p99
                FROM quotes
                WHERE
                    ticker = 'SPX' AND
                    quote_date BETWEEN '2019-01-01' AND '2023-12-31'
                """, Tuple.class)
            .getResultList();

            assertEquals(1, prices.size());
        });

        doInJPA(entityManager -> {
            List<Tuple> prices = entityManager.createNativeQuery("""    
                SELECT DISTINCT
                    YEAR(quote_date) AS year,
                    PERCENTILE_CONT(0.50) WITHIN GROUP(ORDER BY close_price) 
                        OVER (PARTITION BY YEAR(quote_date)) AS median,
                    PERCENTILE_CONT(0.75) WITHIN GROUP(ORDER BY close_price) 
                        OVER (PARTITION BY YEAR(quote_date)) AS p75,
                    PERCENTILE_CONT(0.95) WITHIN GROUP(ORDER BY close_price) 
                        OVER (PARTITION BY YEAR(quote_date)) AS p95,
                    PERCENTILE_CONT(0.99) WITHIN GROUP(ORDER BY close_price) 
                        OVER (PARTITION BY YEAR(quote_date)) AS p99
                FROM quotes
                WHERE
                    ticker = 'SPX' AND
                    quote_date BETWEEN '2019-01-01' AND '2023-12-31'
                ORDER BY year
                """, Tuple.class)
            .getResultList();

            assertEquals(5, prices.size());
        });
    }

    @Entity(name = "quotes")
    public static class Quote {

        @Id
        @Column(length = 4)
        private String ticker;

        @Id
        @Column(name = "quote_date")
        private LocalDate date;

        @Column(name = "close_price", precision = 12, scale = 4)
        private BigDecimal price;

        public String getTicker() {
            return ticker;
        }

        public void setTicker(String ticker) {
            this.ticker = ticker;
        }

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
