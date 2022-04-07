package com.vladmihalcea.book.hpjp.hibernate.naming;

import com.vladmihalcea.book.hpjp.util.AbstractTest;
import com.vladmihalcea.book.hpjp.util.providers.Database;
import org.junit.Test;

import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToOne;
import java.time.LocalDate;
import java.util.Properties;

import static org.junit.Assert.assertEquals;

/**
 * @author Vlad Mihalcea
 */
public class CamelCaseToSnakeCaseNamingStrategyTest extends AbstractTest {

    @Override
    protected Class<?>[] entities() {
        return new Class<?>[] {
                BookAuthor.class,
                PaperbackBook.class,
        };
    }

    @Override
    protected Database database() {
        return Database.POSTGRESQL;
    }

    @Override
    protected void additionalProperties(Properties properties) {
        properties.put(
            "hibernate.physical_naming_strategy",
            CamelCaseToSnakeCaseNamingStrategy.class.getName()
        );
    }

    @Test
    public void test() {
        doInJPA(entityManager -> {
            BookAuthor author = new BookAuthor();
            author.setId(1L);
            author.setFirstName("Vlad");
            author.setLastName("Mihalcea");

            entityManager.persist(author);

            PaperbackBook book = new PaperbackBook();
            book.setISBN("978-9730228236");
            book.setTitle("High-Performance Java Persistence");
            book.setPublishedOn(LocalDate.of(2016, 10, 12));
            book.setPublishedBy(author);

            entityManager.persist(book);
        });

        doInJPA(entityManager -> {
            PaperbackBook book = entityManager.find(PaperbackBook.class, "978-9730228236");
            assertEquals("High-Performance Java Persistence", book.getTitle());

            assertEquals("Vlad Mihalcea", book.getPublishedBy().getFullName());
        });
    }

    @Entity(name = "BookAuthor")
    public static class BookAuthor {

        @Id
        private Long id;

        private String firstName;

        private String lastName;

        public Long getId() {
            return id;
        }

        public void setId(Long id) {
            this.id = id;
        }

        public String getFirstName() {
            return firstName;
        }

        public void setFirstName(String firstName) {
            this.firstName = firstName;
        }

        public String getLastName() {
            return lastName;
        }

        public void setLastName(String lastName) {
            this.lastName = lastName;
        }

        public String getFullName() {
            return firstName + " " + lastName;
        }
    }

    @Entity(name = "PaperbackBook")
    public static class PaperbackBook {

        @Id
        private String ISBN;

        private String title;

        private LocalDate publishedOn;

        @ManyToOne(fetch = FetchType.LAZY)
        private BookAuthor publishedBy;

        public String getISBN() {
            return ISBN;
        }

        public void setISBN(String ISBN) {
            this.ISBN = ISBN;
        }

        public String getTitle() {
            return title;
        }

        public void setTitle(String title) {
            this.title = title;
        }

        public LocalDate getPublishedOn() {
            return publishedOn;
        }

        public void setPublishedOn(LocalDate publishedOn) {
            this.publishedOn = publishedOn;
        }

        public BookAuthor getPublishedBy() {
            return publishedBy;
        }

        public void setPublishedBy(BookAuthor publishedBy) {
            this.publishedBy = publishedBy;
        }
    }
}
