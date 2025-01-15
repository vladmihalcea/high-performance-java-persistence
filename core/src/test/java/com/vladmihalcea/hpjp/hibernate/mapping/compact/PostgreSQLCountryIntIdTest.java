package com.vladmihalcea.hpjp.hibernate.mapping.compact;

import com.vladmihalcea.hpjp.util.AbstractTest;
import com.vladmihalcea.hpjp.util.providers.Database;
import jakarta.persistence.*;
import org.hibernate.cfg.AvailableSettings;
import org.junit.Test;

import java.util.Properties;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * @author Vlad Mihalcea
 */
public class PostgreSQLCountryIntIdTest extends AbstractTest {

    @Override
    protected Class<?>[] entities() {
        return new Class<?>[]{
            Country.class,
            Customer.class
        };
    }

    @Override
    protected Database database() {
        return Database.POSTGRESQL;
    }

    @Override
    protected void additionalProperties(Properties properties) {
        properties.put(AvailableSettings.STATEMENT_BATCH_SIZE, "100");
        properties.put(AvailableSettings.ORDER_INSERTS, Boolean.TRUE);
    }

    @Test
    public void test() {
        doInJPA(entityManager -> {
            entityManager.persist(
                new Country()
                    .setName("Romania")
            );
        });
    }

    @Test
    public void testOverheadImpact() {
        if(!ENABLE_LONG_RUNNING_TESTS) {
            return;
        }
        int customersPerCountry = 100;
        doInJPA(entityManager -> {
            AtomicInteger customerId = new AtomicInteger();
            for (short i = 1; i != 0; i++) {
                Country country = new Country()
                    .setName(String.format("Country no. %d", i));
                entityManager.persist(country);
                for (int j = 1; j <= customersPerCountry; j++) {
                    entityManager.persist(
                        new Customer()
                            .setId(customerId.incrementAndGet())
                            .setCountry(country)
                            .setFirstName("Vlad")
                            .setFirstName("Mihalcea")
                    );
                }
            }
        });

        executeStatement("CREATE INDEX IF NOT EXISTS idx_customer_country_id ON customer (country_id)");
        executeStatement("VACUUM FULL ANALYZE");

        doInJPA(entityManager -> {
            LOGGER.info(
                "Total customer table size: {}",
                entityManager
                    .createNativeQuery("select pg_size_pretty(pg_total_relation_size('customer'))")
                    .getSingleResult()
            );
            LOGGER.info(
                "Total customer index size: {}",
                entityManager
                    .createNativeQuery("select pg_size_pretty(pg_indexes_size('customer'))")
                    .getSingleResult()
            );
        });
    }

    @Entity(name = "Country")
    @Table(name = "country")
    public static class Country {

        @Id
        @GeneratedValue(strategy = GenerationType.SEQUENCE)
        @Column(columnDefinition = "int")
        private Integer id;

        @Column(columnDefinition = "varchar(100)")
        private String name;

        public Integer getId() {
            return id;
        }

        public Country setId(Integer id) {
            this.id = id;
            return this;
        }

        public String getName() {
            return name;
        }

        public Country setName(String name) {
            this.name = name;
            return this;
        }
    }

    @Entity(name = "Customer")
    @Table(name = "customer")
    public class Customer {

        @Id
        private Integer id;

        @Column(name = "first_name", columnDefinition = "varchar(100)")
        private String firstName;

        @Column(name = "last_name", columnDefinition = "varchar(100)")
        private String lastName;
        
        @ManyToOne(fetch = FetchType.LAZY)
        @JoinColumn(name = "country_id")
        private Country country;

        public Integer getId() {
            return id;
        }

        public Customer setId(Integer id) {
            this.id = id;
            return this;
        }

        public String getFirstName() {
            return firstName;
        }

        public Customer setFirstName(String firstName) {
            this.firstName = firstName;
            return this;
        }

        public String getLastName() {
            return lastName;
        }

        public Customer setLastName(String lastName) {
            this.lastName = lastName;
            return this;
        }

        public Country getCountry() {
            return country;
        }

        public Customer setCountry(Country country) {
            this.country = country;
            return this;
        }
    }
}
