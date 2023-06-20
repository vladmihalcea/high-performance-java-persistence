package com.vladmihalcea.hpjp.hibernate.mapping.compact;

import com.vladmihalcea.hpjp.util.AbstractTest;
import com.vladmihalcea.hpjp.util.providers.Database;
import jakarta.persistence.*;
import org.junit.Test;

/**
 * @author Vlad Mihalcea
 */
public class CountryShortIdTest extends AbstractTest {

    @Override
    protected Class<?>[] entities() {
        return new Class<?>[]{
            Country.class
        };
    }

    @Override
    protected Database database() {
        return Database.SQLSERVER;
    }

    @Test
    public void test() {
        doInJPA(entityManager -> {
            entityManager.persist(
                new Country()
                    .setName("")
            );
        });
    }

    public enum PostStatus {
        PENDING,
        APPROVED,
        SPAM,
        REQUIRES_MODERATOR_INTERVENTION
    }

    @Entity(name = "Country")
    public static class Country {

        @Id
        @GeneratedValue(strategy = GenerationType.SEQUENCE)
        @Column(columnDefinition = "SMALLINT")
        private Short id;

        @Column(columnDefinition = "VARCHAR(100)")
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
}
