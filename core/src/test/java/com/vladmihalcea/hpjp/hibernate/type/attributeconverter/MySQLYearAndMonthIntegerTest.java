package com.vladmihalcea.hpjp.hibernate.type.attributeconverter;

import com.vladmihalcea.hpjp.util.AbstractMySQLIntegrationTest;
import io.hypersistence.utils.hibernate.type.basic.Iso8601MonthType;
import io.hypersistence.utils.hibernate.type.basic.YearType;
import jakarta.persistence.*;
import org.hibernate.Session;
import org.hibernate.annotations.NaturalId;
import org.hibernate.annotations.Type;
import org.junit.Test;

import java.time.Month;
import java.time.Year;
import java.util.Properties;

import static org.junit.Assert.assertEquals;

/**
 * @author Vlad Mihalcea
 */
public class MySQLYearAndMonthIntegerTest extends AbstractMySQLIntegrationTest {

    @Override
    protected Class<?>[] entities() {
        return new Class<?>[]{
            Book.class
        };
    }

    @Override
    protected void beforeInit() {
        executeStatement("drop table if exists book");
        executeStatement("create table book (publishing_month tinyint, publishing_year int, id bigint not null auto_increment, isbn varchar(255), title varchar(255), primary key (id)) engine=InnoDB");
        executeStatement("alter table book add constraint UK_book_isbn unique (isbn)");
    }

    @Override
    protected void additionalProperties(Properties properties) {
        properties.put("hibernate.hbm2ddl.auto", "validate");
    }

    @Test
    public void test() {
        doInJPA(entityManager -> {
            Book book = new Book();
            book.setIsbn("978-9730228236");
            book.setTitle("High-Performance Java Persistence");
            book.setPublishingYear(Year.of(2016));
            book.setPublishingMonth(Month.of(10));

            entityManager.persist(book);
        });

        doInJPA(entityManager -> {
            Book book = entityManager
                    .unwrap(Session.class)
                    .bySimpleNaturalId(Book.class)
                    .load("978-9730228236");

            assertEquals(Year.of(2016), book.getPublishingYear());
            assertEquals(Month.of(10), book.getPublishingMonth());
        });

        doInJPA(entityManager -> {
            Book book = entityManager.createQuery("""
                select b
                from Book b
                where
                   b.title = :title and
                   b.publishingYear = :publishingYear and
                   b.publishingMonth = :publishingMonth
                """, Book.class)
            .setParameter("title", "High-Performance Java Persistence")
            .setParameter("publishingYear", Year.of(2016))
            .setParameter("publishingMonth", Month.of(10))
            .getSingleResult();

            assertEquals("978-9730228236", book.getIsbn());
        });
    }


    @Entity(name = "Book")
    @Table(name = "book")
    public static class Book {

        @Id
        @GeneratedValue(strategy = GenerationType.IDENTITY)
        private Long id;

        @NaturalId
        private String isbn;

        private String title;

        @Column(name = "publishing_year")
        @Type(YearType.class)
        private Year publishingYear;

        @Column(name = "publishing_month", columnDefinition = "tinyint")
        @Type(Iso8601MonthType.class)
        private Month publishingMonth;

        public Long getId() {
            return id;
        }

        public void setId(Long id) {
            this.id = id;
        }

        public String getIsbn() {
            return isbn;
        }

        public void setIsbn(String isbn) {
            this.isbn = isbn;
        }

        public String getTitle() {
            return title;
        }

        public void setTitle(String title) {
            this.title = title;
        }

        public Year getPublishingYear() {
            return publishingYear;
        }

        public void setPublishingYear(Year publishingYear) {
            this.publishingYear = publishingYear;
        }

        public Month getPublishingMonth() {
            return publishingMonth;
        }

        public void setPublishingMonth(Month publishingMonth) {
            this.publishingMonth = publishingMonth;
        }
    }
}
