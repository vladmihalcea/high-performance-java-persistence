package com.vladmihalcea.book.hpjp.hibernate.type;

import com.vladmihalcea.book.hpjp.util.AbstractPostgreSQLIntegrationTest;
import com.vladmihalcea.hibernate.type.basic.YearMonthDateType;
import com.vladmihalcea.hibernate.type.interval.PostgreSQLIntervalType;
import jakarta.persistence.*;
import org.hibernate.Session;
import org.hibernate.annotations.NaturalId;
import org.hibernate.annotations.Type;
import org.hibernate.query.NativeQuery;
import org.junit.Test;

import java.time.Duration;
import java.time.LocalDate;
import java.time.Month;
import java.time.YearMonth;

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
            Tuple result = (Tuple) entityManager
                .createNativeQuery(
                    "SELECT " +
                    "   b.published_on AS published_on, " +
                    "   b.presale_period  AS presale_period " +
                    "FROM " +
                    "   book b " +
                    "WHERE " +
                    "   b.isbn = :isbn ", Tuple.class)
            .setParameter("isbn", "978-9730228236")
            .unwrap(NativeQuery.class)
            .addScalar("published_on", YearMonth.class)
            .addScalar("presale_period", Duration.class)
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
