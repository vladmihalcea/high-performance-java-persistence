package com.vladmihalcea.hpjp.hibernate.type.datetime;

import com.fasterxml.jackson.databind.JsonNode;
import com.vladmihalcea.hpjp.util.AbstractPostgreSQLIntegrationTest;
import io.hypersistence.utils.hibernate.type.basic.YearMonthDateType;
import io.hypersistence.utils.hibernate.type.interval.PostgreSQLIntervalType;
import io.hypersistence.utils.hibernate.type.json.JsonStringType;
import jakarta.persistence.*;
import org.hibernate.Session;
import org.hibernate.annotations.NaturalId;
import org.hibernate.annotations.Type;
import org.hibernate.jpa.boot.spi.TypeContributorList;
import org.hibernate.query.NativeQuery;
import org.junit.Test;

import java.time.Duration;
import java.time.LocalDate;
import java.time.Month;
import java.time.YearMonth;
import java.util.Collections;
import java.util.Properties;

import static org.hibernate.jpa.boot.internal.EntityManagerFactoryBuilderImpl.TYPE_CONTRIBUTORS;
import static org.junit.Assert.assertEquals;

/**
 * @author Vlad Mihalcea
 */
public class PostgreSQLIntervalDurationTest extends AbstractPostgreSQLIntegrationTest {

    @Override
    protected Class<?>[] entities() {
        return new Class<?>[]{
            Book.class
        };
    }

    @Override
    protected void additionalProperties(Properties properties) {
        properties.put(TYPE_CONTRIBUTORS,
            (TypeContributorList) () -> Collections.singletonList(
                (typeContributions, serviceRegistry) -> {
                    typeContributions.contributeType(YearMonthDateType.INSTANCE);
                }
            )
        );
    }

    @Test
    public void test() {
        doInJPA(entityManager -> {
            entityManager.persist(
                new Book()
                    .setIsbn("978-9730228236")
                    .setTitle("High-Performance Java Persistence")
                    .setPublishedOn(YearMonth.of(2016, 10))
                    .setPresalePeriod(
                        Duration.between(
                            LocalDate.of(2015, Month.NOVEMBER, 2).atStartOfDay(),
                            LocalDate.of(2016, Month.AUGUST, 25).atStartOfDay()
                        )
                    )
            );
        });

        doInJPA(entityManager -> {
            Book book = entityManager
                .unwrap(Session.class)
                .bySimpleNaturalId(Book.class)
                .load("978-9730228236");

            assertEquals(
                Duration.between(
                    LocalDate.of(2015, Month.NOVEMBER, 2).atStartOfDay(),
                    LocalDate.of(2016, Month.AUGUST, 25).atStartOfDay()
                ),
                book.getPresalePeriod()
            );
        });

        doInJPA(entityManager -> {
            Tuple result = entityManager.createQuery("""
                SELECT
                   b.publishedOn AS published_on,
                   b.presalePeriod  AS presale_period
                FROM
                   Book b
                WHERE
                   b.isbn = :isbn
                """, Tuple.class)
            .setParameter("isbn", "978-9730228236")
            .getSingleResult();

            assertEquals(
                YearMonth.of(2016, 10),
                result.get("published_on")
            );

            assertEquals(
                Duration.between(
                    LocalDate.of(2015, Month.NOVEMBER, 2).atStartOfDay(),
                    LocalDate.of(2016, Month.AUGUST, 25).atStartOfDay()
                ),
                result.get("presale_period")
            );
        });
    }

    @Entity(name = "Book")
    @Table(name = "book")
    public static class Book {

        @Id
        @GeneratedValue
        private Long id;

        @NaturalId
        private String isbn;

        private String title;

        @Type(YearMonthDateType.class)
        @Column(name = "published_on", columnDefinition = "date")
        private YearMonth publishedOn;

        @Type(PostgreSQLIntervalType.class)
        @Column(name = "presale_period", columnDefinition = "interval")
        private Duration presalePeriod;

        public Long getId() {
            return id;
        }

        public Book setId(Long id) {
            this.id = id;
            return this;
        }

        public String getIsbn() {
            return isbn;
        }

        public Book setIsbn(String isbn) {
            this.isbn = isbn;
            return this;
        }

        public String getTitle() {
            return title;
        }

        public Book setTitle(String title) {
            this.title = title;
            return this;
        }

        public YearMonth getPublishedOn() {
            return publishedOn;
        }

        public Book setPublishedOn(YearMonth publishedOn) {
            this.publishedOn = publishedOn;
            return this;
        }

        public Duration getPresalePeriod() {
            return presalePeriod;
        }

        public Book setPresalePeriod(Duration presalePeriod) {
            this.presalePeriod = presalePeriod;
            return this;
        }
    }
}
