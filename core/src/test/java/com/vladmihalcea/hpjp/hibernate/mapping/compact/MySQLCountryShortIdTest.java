package com.vladmihalcea.hpjp.hibernate.mapping.compact;

import com.vladmihalcea.hpjp.util.AbstractTest;
import com.vladmihalcea.hpjp.util.providers.DataSourceProvider;
import com.vladmihalcea.hpjp.util.providers.Database;
import com.vladmihalcea.hpjp.util.providers.MySQLDataSourceProvider;
import jakarta.persistence.*;
import org.hibernate.cfg.AvailableSettings;
import org.junit.Test;

import java.util.Properties;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * @author Vlad Mihalcea
 */
public class MySQLCountryShortIdTest extends AbstractTest {

    @Override
    protected Class<?>[] entities() {
        return new Class<?>[]{
            Country.class,
            Customer.class
        };
    }

    @Override
    protected Database database() {
        return Database.MYSQL;
    }

    @Override
    protected void additionalProperties(Properties properties) {
        properties.put(AvailableSettings.STATEMENT_BATCH_SIZE, "1000");
        properties.put(AvailableSettings.ORDER_INSERTS, Boolean.TRUE);
    }

    @Override
    protected DataSourceProvider dataSourceProvider() {
        return new MySQLDataSourceProvider().setRewriteBatchedStatements(true);
    }

    @Test
    public void test() {
        doInJPA(entityManager -> {
            entityManager.persist(
                new Country()
                    .setId((short) 1)
                    .setName("Romania")
            );
        });
    }

    @Test
    public void testOverheadImpact() {
        if(!ENABLE_LONG_RUNNING_TESTS) {
            return;
        }
        int customersPerCountry = 25_000;
        doInJPA(entityManager -> {
            AtomicInteger customerId = new AtomicInteger();
            for (short i = 1; i <= 200; i++) {
                Country country = new Country()
                    .setId(i)
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

        executeStatement("CREATE INDEX idx_customer_country_id ON customer (country_id)");
        executeQuery("ANALYZE TABLE customer");

        doInJPA(entityManager -> {
            LOGGER.info(
                "Total customer table size: {} MB",
                entityManager
                    .createNativeQuery("""
                        select
                            ROUND(((DATA_LENGTH + INDEX_LENGTH) / 1024 / 1024), 2)
                        from information_schema.TABLES
                        where TABLE_SCHEMA = 'high_performance_java_persistence' AND TABLE_NAME = 'customer'
                        """)
                    .getSingleResult()
            );
        });

        doInJPA(entityManager -> {
            LOGGER.info(
                "Total customer index size: {} MB",
                entityManager
                    .createNativeQuery("""
                        select
                            ROUND((INDEX_LENGTH / 1024 / 1024), 2)
                        from information_schema.TABLES
                        where TABLE_SCHEMA = 'high_performance_java_persistence' AND TABLE_NAME = 'customer'
                        """)
                    .getSingleResult()
            );
        });
    }

    @Entity(name = "Country")
    @Table(name = "country")
    public static class Country {

        @Id
        @Column(columnDefinition = "smallint unsigned")
        private Short id;

        @Column(columnDefinition = "varchar(100)")
        private String name;

        public Short getId() {
            return id;
        }

        public Country setId(Short id) {
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
    @Table(name = "customer", indexes = @Index(name ="FK_customer_country_id", columnList = "country_id"))
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
