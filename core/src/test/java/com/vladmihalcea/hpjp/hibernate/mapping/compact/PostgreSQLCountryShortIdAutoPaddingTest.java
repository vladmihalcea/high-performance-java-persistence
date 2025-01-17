package com.vladmihalcea.hpjp.hibernate.mapping.compact;

import com.vladmihalcea.hpjp.util.AbstractTest;
import com.vladmihalcea.hpjp.util.providers.Database;
import jakarta.persistence.*;
import org.hibernate.cfg.AvailableSettings;
import org.hibernate.tool.schema.Action;
import org.junit.Test;

import java.util.Properties;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * @author Vlad Mihalcea
 */
public class PostgreSQLCountryShortIdAutoPaddingTest extends AbstractTest {

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
        properties.put(AvailableSettings.HBM2DDL_AUTO, Action.NONE.getExternalHbm2ddlName());
        properties.put(AvailableSettings.STATEMENT_BATCH_SIZE, "100");
        properties.put(AvailableSettings.ORDER_INSERTS, Boolean.TRUE);
    }

    @Override
    protected void beforeInit() {
        executeStatement("alter table if exists customer drop constraint if exists FK_customer_country_id");
        executeStatement("drop table if exists customer cascade");
        executeStatement("drop table if exists country cascade");
        executeStatement("drop sequence if exists country_SEQ");
        executeStatement("create sequence country_SEQ start with 1 increment by 50");
        executeStatement("create table country (id smallint not null, name varchar(100), primary key (id))");
        executeStatement("create table customer (country_id smallint, id integer not null, first_name varchar(100), last_name varchar(100), primary key (id))");
        executeStatement("alter table if exists customer add constraint FK_customer_country_id foreign key (country_id) references country");
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
                    .createNativeQuery("select pg_size_pretty(pg_table_size('idx_customer_country_id'))")
                    .getSingleResult()
            );
        });
    }

    @Entity(name = "Country")
    @Table(name = "country")
    public static class Country {

        @Id
        @GeneratedValue(strategy = GenerationType.SEQUENCE)
        @Column(columnDefinition = "smallint")
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
