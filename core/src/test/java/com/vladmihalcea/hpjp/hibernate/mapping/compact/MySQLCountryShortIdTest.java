package com.vladmihalcea.hpjp.hibernate.mapping.compact;

import com.vladmihalcea.hpjp.util.AbstractTest;
import com.vladmihalcea.hpjp.util.providers.Database;
import jakarta.persistence.*;
import org.junit.Test;

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

    @Test
    public void test() {
        doInJPA(entityManager -> {
            entityManager.persist(
                new Country()
                    .setName("Romania")
            );
        });
    }

    @Entity(name = "Country")
    @Table(name = "country")
    public static class Country {

        @Id
        @GeneratedValue(strategy = GenerationType.IDENTITY)
        @Column(columnDefinition = "tinyint unsigned")
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
