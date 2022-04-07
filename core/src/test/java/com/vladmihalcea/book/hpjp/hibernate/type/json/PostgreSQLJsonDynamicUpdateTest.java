package com.vladmihalcea.book.hpjp.hibernate.type.json;

import com.vladmihalcea.book.hpjp.util.AbstractPostgreSQLIntegrationTest;
import com.vladmihalcea.hibernate.type.json.JsonType;
import jakarta.persistence.*;
import org.hibernate.Session;
import org.hibernate.annotations.DynamicUpdate;
import org.hibernate.annotations.NaturalId;
import org.hibernate.annotations.Type;
import org.junit.Test;

/**
 * @author Vlad Mihalcea
 */
public class PostgreSQLJsonDynamicUpdateTest extends AbstractPostgreSQLIntegrationTest {

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
                    .setIsbn("978-9730228236")
                    .setTitle("High-Performance Java Persistence")
                    .setAuthor("Vlad Mihalcea")
                    .setProperties("""
                        {
                           "publisher": "Amazon",
                           "price": 44.99,
                           "reviews": [
                               {
                                   "reviewer": "Cristiano",
                                   "review": "Excellent book to understand Java Persistence",
                                   "date": "2017-11-14",
                                   "rating": 5
                               },
                               {
                                   "reviewer": "T.W",
                                   "review": "The best JPA ORM book out there",
                                   "date": "2019-01-27",
                                   "rating": 5
                               },
                               {
                                   "reviewer": "Shaikh",
                                   "review": "The most informative book",
                                   "date": "2016-12-24",
                                   "rating": 4
                               }
                           ]
                        }
                        """)
            );
        });
    }

    @Test
    public void testFetchAndUpdateOtherAttribute() {
        doInJPA(entityManager -> {
            entityManager
                .unwrap(Session.class)
                .bySimpleNaturalId(Book.class)
                .load("978-9730228236")
                .setTitle("High-Performance Java Persistence, 2nd edition");
        });
    }

    @Entity(name = "Book")
    @Table(name = "book")
    @DynamicUpdate
    public static class Book {

        @Id
        @GeneratedValue
        private Long id;

        @NaturalId
        private String isbn;

        private String title;

        private String author;

        @Column(columnDefinition = "jsonb")
        @Type(JsonType.class)
        private String properties;

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

        public String getProperties() {
            return properties;
        }

        public Book setProperties(String properties) {
            this.properties = properties;
            return this;
        }
    }
}
