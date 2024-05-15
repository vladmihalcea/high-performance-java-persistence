package com.vladmihalcea.hpjp.hibernate.index.postgres.hot;

import com.vladmihalcea.hpjp.util.AbstractPostgreSQLIntegrationTest;
import io.hypersistence.utils.hibernate.type.json.JsonType;
import jakarta.persistence.*;
import org.hibernate.annotations.DynamicUpdate;
import org.hibernate.annotations.Type;
import org.junit.Test;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.HashMap;
import java.util.Map;
import java.util.TreeMap;

/**
 * @author Vlad Mihalcea
 */
public class PostgreSQLHOTDynamicUpdateTest extends AbstractPostgreSQLIntegrationTest {

    @Override
    protected Class<?>[] entities() {
        return new Class[]{
            Book.class,
            Author.class
        };
    }

    @Override
    protected void afterInit() {
        doInJPA(entityManager -> {
            Author author = new Author()
                .setId(1L)
                .setFirstName("Vlad")
                .setLastName("Mihalcea")
                .setCountry("România");

            entityManager.persist(author);
            
            Book book = new Book()
                .setIsbn("978-9730228236")
                .setTitle("High-Performance Java Persistence")
                .setAuthor(author)
                .addProperty("title", "High-Performance Java Persistence")
                .addProperty("author", "Vlad Mihalcea")
                .addProperty("publisher", "Amazon")
                .addProperty("price", "$44.95");
            entityManager.persist(
                book
            );
        });
        executeStatement(
            "DROP INDEX IF EXISTS idx_book_author_id",
            "DROP INDEX IF EXISTS idx_book_isbn",
            """
            CREATE INDEX IF NOT EXISTS idx_book_isbn ON book (isbn)
            """,
            """
            CREATE INDEX idx_book_author_id ON book (author_id)
            """,
            "ANALYZE VERBOSE"
        );
    }

    @Test
    public void testHOT() {
        if(!ENABLE_LONG_RUNNING_TESTS) {
            return;
        }
        for (int i = 1; i <= 50; i++) {
            final int revision = i;
            checkHeapOnlyTuples();
            doInJPA(entityManager -> {
                Book book = entityManager.createQuery("""
                    select b
                    from Book b
                    where b.isbn  = :isbn
                    """, Book.class)
                .setParameter("isbn", "978-9730228236")
                .getSingleResult();

                book.setTitle(
                    String.format(
                        "High-Performance Java Persistence, revision %d",
                        revision
                    )
                );

                Map<String, String> props = book.getProperties();
                book.setProperties(
                    revision % 2 == 0 ? new HashMap<>(props) : new TreeMap<>(props)
                );
            });
            checkHeapOnlyTuples();
        }
    }

    private void checkHeapOnlyTuples() {
        doInJDBC(connection -> {
            try {
                try (Statement statement = connection.createStatement()) {
                    ResultSet resultSet = statement.executeQuery(
                        """
                        SELECT n_tup_upd, n_tup_hot_upd
                        FROM pg_stat_user_tables
                        WHERE relname = 'book'
                        """
                    );
                    while (resultSet.next()) {
                        int i = 0;
                        long n_tup_upd = resultSet.getLong(++i);
                        long n_tup_hot_upd = resultSet.getLong(++i);

                        LOGGER.info("HOT: n_tup_upd: {}, n_tup_hot_upd: {}", n_tup_upd, n_tup_hot_upd);
                    }
                }
            } catch (SQLException e) {
                throw new IllegalStateException(e);
            }
        });

    }

    @Entity(name = "Book")
    @Table(name = "book")
    //@DynamicUpdate
    public static class Book {

        @Id
        @GeneratedValue
        private Long id;

        private String isbn;

        private String title;

        @ManyToOne(fetch = FetchType.LAZY)
        private Author author;

        @Column(name = "price_in_cents")
        private int priceInCents;

        private String publisher;

        @Column(columnDefinition = "json")
        @Type(JsonType.class)
        private Map<String, String> properties = new HashMap<>();
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

        public Author getAuthor() {
            return author;
        }

        public Book setAuthor(Author author) {
            this.author = author;
            return this;
        }

        public int getPriceInCents() {
            return priceInCents;
        }

        public Book setPriceInCents(int priceInCents) {
            this.priceInCents = priceInCents;
            return this;
        }

        public String getPublisher() {
            return publisher;
        }

        public Book setPublisher(String publisher) {
            this.publisher = publisher;
            return this;
        }

        public Map<String, String> getProperties() {
            return properties;
        }

        public Book setProperties(Map<String, String> properties) {
            this.properties = properties;
            return this;
        }

        public Book addProperty(String key, String value) {
            properties.put(key, value);
            return this;
        }
    }

    @Entity(name = "Author")
    @Table(name = "author")
    @DynamicUpdate
    public static class Author {

        @Id
        private Long id;

        @Column(name = "first_name")
        private String firstName;

        @Column(name = "last_name")
        private String lastName;

        private String country;

        @Column(name = "tax_treaty_claiming")
        private boolean taxTreatyClaiming;

        public Long getId() {
            return id;
        }

        public Author setId(Long id) {
            this.id = id;
            return this;
        }

        public String getFirstName() {
            return firstName;
        }

        public Author setFirstName(String firstName) {
            this.firstName = firstName;
            return this;
        }

        public String getLastName() {
            return lastName;
        }

        public Author setLastName(String lastName) {
            this.lastName = lastName;
            return this;
        }

        public String getCountry() {
            return country;
        }

        public Author setCountry(String country) {
            this.country = country;
            return this;
        }

        public boolean isTaxTreatyClaiming() {
            return taxTreatyClaiming;
        }

        public Author setTaxTreatyClaiming(boolean taxTreatyClaiming) {
            this.taxTreatyClaiming = taxTreatyClaiming;
            return this;
        }
    }
}
