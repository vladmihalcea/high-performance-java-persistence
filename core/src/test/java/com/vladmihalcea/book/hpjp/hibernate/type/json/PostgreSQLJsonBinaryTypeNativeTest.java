package com.vladmihalcea.book.hpjp.hibernate.type.json;

import com.vladmihalcea.book.hpjp.util.AbstractPostgreSQLIntegrationTest;
import com.vladmihalcea.hibernate.type.json.JsonBinaryType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import org.hibernate.annotations.Type;
import org.junit.Test;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.junit.Assert.*;

/**
 * @author Vlad Mihalcea
 */
public class PostgreSQLJsonBinaryTypeNativeTest extends AbstractPostgreSQLIntegrationTest {

    @Override
    protected Class<?>[] entities() {
        return new Class<?>[]{
            Book.class
        };
    }

    @Override
    protected void afterInit() {
        doInJPA(entityManager -> {
            entityManager.persist(
                new Book()
                    .setId(1L)
                    .setIsbn("978-9730228236")
                    .setTitle("High-Performance Java Persistence")
                    .setAuthor("Vlad Mihalcea")
                    .setPrice(new BigDecimal("44.99"))
                    .setPublisher("Amazon KDP")
            );
        });
    }

    @Test
    public void testBasicParameters() {
        doInJPA(entityManager -> {
            Book book = entityManager.createQuery("""
                select b
                from Book b
                where b.isbn = :isbn
                """,
                Book.class)
            .setParameter("isbn", "978-9730228236")
            .getSingleResult();

            assertEquals("High-Performance Java Persistence", book.title);
        });

        doInJPA(entityManager -> {
            List<Book> books = entityManager.createQuery("""
                select b
                from Book b
                where b.publisher in (:publishers)
                """,
                Book.class)
            .setParameter(
                "publishers",
                Arrays.asList(
                    "O'Reilly",
                    "Manning",
                    "Amazon KDP"
                )
            )
            .getResultList();

            assertEquals(1, books.size());
        });
    }

    @Test
    public void testUpdateListUsingNativeSQL() {

        doInJPA(entityManager -> {
            Book book = entityManager.find(Book.class, 1L);

            assertTrue(book.getReviews().isEmpty());

            int updateCount = entityManager.createNativeQuery("""
                UPDATE 
                    book
                SET 
                    reviews = :reviews
                WHERE 
                    isbn = :isbn AND
                    jsonb_array_length(reviews) = 0             
                """)
                .setParameter("isbn", "978-9730228236")
                .unwrap(org.hibernate.query.Query.class)
                .setParameter(
                    "reviews",
                    Arrays.asList(
                        new BookReview()
                            .setReview("Excellent book to understand Java Persistence")
                            .setRating(5),
                        new BookReview()
                            .setReview("The best JPA ORM book out there")
                            .setRating(5)
                    ),
                    JsonBinaryType.INSTANCE
                )
                .executeUpdate();

            entityManager.refresh(book);

            assertEquals(2, book.getReviews().size());
        });
    }

    @Test
    public void testUpdateSerializableUsingNativeSQL() {

        doInJPA(entityManager -> {
            Book book = entityManager.find(Book.class, 1L);

            assertNull(book.getProperties());

            int updateCount = entityManager.createNativeQuery("""
                UPDATE 
                    book
                SET 
                    properties = :properties
                WHERE 
                    isbn = :isbn AND
                    properties ->> 'weight' is null             
                """)
                .setParameter("isbn", "978-9730228236")
                .unwrap(org.hibernate.query.Query.class)
                .setParameter(
                    "properties",
                    new BookProperties()
                        .setWidth(new BigDecimal("8.5"))
                        .setHeight(new BigDecimal("11"))
                        .setWeight(new BigDecimal("2.5")),
                    JsonBinaryType.INSTANCE
                )
                .executeUpdate();

            entityManager.refresh(book);

            BookProperties properties = book.getProperties();
            assertEquals(new BigDecimal("2.5"), properties.getWeight());
        });
    }

    @Entity(name = "Book")
    @Table(name = "book")
    public static class Book {

        @Id
        private Long id;

        private String isbn;

        private String title;

        private String author;

        private String publisher;

        private BigDecimal price;

        @Type(JsonBinaryType.class)
        @Column(columnDefinition = "jsonb")
        private List<BookReview> reviews = new ArrayList<>();

        @Type(JsonBinaryType.class)
        @Column(columnDefinition = "jsonb")
        private BookProperties properties;

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

        public String getAuthor() {
            return author;
        }

        public Book setAuthor(String author) {
            this.author = author;
            return this;
        }

        public String getPublisher() {
            return publisher;
        }

        public Book setPublisher(String publisher) {
            this.publisher = publisher;
            return this;
        }

        public BigDecimal getPrice() {
            return price;
        }

        public Book setPrice(BigDecimal price) {
            this.price = price;
            return this;
        }

        public List<BookReview> getReviews() {
            return reviews;
        }

        public Book setReviews(List<BookReview> reviews) {
            this.reviews = reviews;
            return this;
        }

        public BookProperties getProperties() {
            return properties;
        }

        public Book setProperties(BookProperties properties) {
            this.properties = properties;
            return this;
        }
    }

    public static class BookReview implements Serializable {

        private String review;

        private int rating;

        public String getReview() {
            return review;
        }

        public BookReview setReview(String review) {
            this.review = review;
            return this;
        }

        public int getRating() {
            return rating;
        }

        public BookReview setRating(int rating) {
            this.rating = rating;
            return this;
        }
    }

    public static class BookProperties implements Serializable {

        private BigDecimal width;

        private BigDecimal height;

        private BigDecimal weight;

        public BigDecimal getWidth() {
            return width;
        }

        public BookProperties setWidth(BigDecimal width) {
            this.width = width;
            return this;
        }

        public BigDecimal getHeight() {
            return height;
        }

        public BookProperties setHeight(BigDecimal height) {
            this.height = height;
            return this;
        }

        public BigDecimal getWeight() {
            return weight;
        }

        public BookProperties setWeight(BigDecimal weight) {
            this.weight = weight;
            return this;
        }
    }
}
