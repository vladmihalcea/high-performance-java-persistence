package com.vladmihalcea.book.hpjp.hibernate.fetching;

import com.vladmihalcea.book.hpjp.util.AbstractTest;
import org.hibernate.NonUniqueObjectException;
import org.hibernate.Session;
import org.junit.Test;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.ParameterExpression;
import jakarta.persistence.criteria.Root;

import java.util.Arrays;
import java.util.List;
import java.util.Properties;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;

public class FindByMultipleIdsTest extends AbstractTest {

    @Override
    protected Class<?>[] entities() {
        return new Class<?>[]{
                Book.class
        };
    }

    @Override
    protected void additionalProperties(Properties properties) {
        properties.put("hibernate.query.in_clause_parameter_padding", "true");
    }

    @Override
    protected void afterInit() {
        doInJPA(entityManager -> {
            entityManager.persist(
                new Book()
                    .setIsbn("978-9730228236")
                    .setTitle("High-Performance Java Persistence")
                    .setAuthor("Vlad Mihalcea")
            );

            entityManager.persist(
                new Book()
                    .setIsbn("978-1934356555")
                    .setTitle("SQL Antipatterns")
                    .setAuthor("Bill Karwin")
            );

            entityManager.persist(
                new Book()
                    .setIsbn("978-3950307825")
                    .setTitle("SQL Performance Explained")
                    .setAuthor("Markus Winand")
            );

            entityManager.persist(
                new Book()
                    .setIsbn("978-1449373320")
                    .setTitle("Designing Data-Intensive Applications")
                    .setAuthor("Martin Kleppmann")
            );
        });
    }

    @Test
    public void testJPQL() {
        doInJPA(entityManager -> {
            List<Book> books = entityManager.createQuery("""
                select b
                from Book b
                where b.isbn in (:isbn)
                """, Book.class)
            .setParameter("isbn", Arrays.asList(
                "978-9730228236",
                "978-1934356555",
                "978-3950307825"
            ))
            .getResultList();

            assertEquals(3, books.size());
        });
    }

    @Test
    public void testCriteriaAPI() {
        doInJPA(entityManager -> {
            CriteriaBuilder builder = entityManager.getCriteriaBuilder();
            CriteriaQuery<Book> query = builder.createQuery(Book.class);
            Root<Book> root = query.from(Book.class);

            ParameterExpression<List> isbn = builder.parameter(List.class);
            query.where(root.get("isbn").in(isbn));

            List<Book> books = entityManager
                .createQuery(query)
                .setParameter(isbn, Arrays.asList(
                    "978-9730228236",
                    "978-1934356555",
                    "978-3950307825"
                ))
                .getResultList();

            assertEquals(3, books.size());
        });
    }

    @Test
    public void testByMultipleIds() {
        doInJPA(entityManager -> {
            Session session = entityManager.unwrap(Session.class);

            List<Book> books = session.byMultipleIds(Book.class)
                .multiLoad(
                    "978-9730228236",
                    "978-1934356555",
                    "978-3950307825"
                );

            assertEquals(3, books.size());
        });

        doInJPA(entityManager -> {
            Session session = entityManager.unwrap(Session.class);

            List<Book> books = session.byMultipleIds(Book.class)
                .multiLoad(
                    List.of(
                        "978-9730228236",
                        "978-1934356555",
                        "978-3950307825"
                    )
                );

            assertEquals(3, books.size());
        });
    }

    @Entity(name = "Book")
    @Table(name = "book")
    public static class Book {

        @Id
        private String isbn;

        private String title;

        private String author;

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

        public String getAuthor() {
            return author;
        }

        public Book setAuthor(String author) {
            this.author = author;
            return this;
        }
    }
}
