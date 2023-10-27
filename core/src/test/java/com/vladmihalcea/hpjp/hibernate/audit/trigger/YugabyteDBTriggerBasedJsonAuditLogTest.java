package com.vladmihalcea.hpjp.hibernate.audit.trigger;

import com.vladmihalcea.hpjp.util.AbstractTest;
import com.vladmihalcea.hpjp.util.providers.Database;
import jakarta.persistence.*;
import org.hibernate.Session;
import org.hibernate.annotations.DynamicUpdate;
import org.hibernate.dialect.Dialect;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.junit.Ignore;
import org.junit.Test;

import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.Assert.assertEquals;

/**
 * @author Vlad Mihalcea
 */
public class YugabyteDBTriggerBasedJsonAuditLogTest extends AbstractTest {

    @Override
    protected Class<?>[] entities() {
        return new Class[]{
            Book.class,
            Author.class
        };
    }

    @Override
    protected Database database() {
        return Database.YUGABYTEDB;
    }

    @Override
    public void init() {
        if(!ENABLE_LONG_RUNNING_TESTS) {
            return;
        }
        super.init();
    }

    @Override
    protected void afterInit() {
        executeStatement("DROP TYPE dml_type CASCADE");
        executeStatement("CREATE TYPE dml_type AS ENUM ('INSERT', 'UPDATE', 'DELETE')");

        executeStatement("DROP TABLE IF EXISTS audit_log CASCADE");
        executeStatement(
            String.format(
                """
                CREATE TABLE IF NOT EXISTS audit_log (
                    table_name varchar(255) NOT NULL,
                    row_id bigint NOT NULL,
                    old_row_data jsonb,
                    new_row_data jsonb,
                    dml_type dml_type NOT NULL,
                    dml_timestamp timestamp NOT NULL,
                    dml_created_by varchar(255) NOT NULL,
                    trx_timestamp timestamp NOT NULL,
                    PRIMARY KEY (%s, dml_type, dml_timestamp)
                )
                """,
                database() == Database.YUGABYTEDB ? "(table_name, row_id) HASH" : "table_name, row_id"
            )
        );

        executeStatement("DROP FUNCTION IF EXISTS audit_log_trigger_function cascade");

        executeStatement("""          
            CREATE OR REPLACE FUNCTION audit_log_trigger_function()
            RETURNS trigger AS $body$
            BEGIN
               if (TG_OP = 'INSERT') then
                   INSERT INTO audit_log (
                       table_name,
                       row_id,
                       old_row_data,
                       new_row_data,
                       dml_type,
                       dml_timestamp,
                       dml_created_by,
                       trx_timestamp
                   )
                   VALUES(
                       TG_TABLE_NAME,
                       NEW.id,
                       null,
                       to_jsonb(NEW),
                       'INSERT',
                       statement_timestamp(),
                       current_setting('var.logged_user'),
                       transaction_timestamp()
                   );
                        
                   RETURN NEW;
               elsif (TG_OP = 'UPDATE') then
                   INSERT INTO audit_log (
                       table_name,
                       row_id,
                       old_row_data,
                       new_row_data,
                       dml_type,
                       dml_timestamp,
                       dml_created_by,
                       trx_timestamp
                   )
                   VALUES(
                       TG_TABLE_NAME,
                       NEW.id,
                       to_jsonb(OLD),
                       to_jsonb(NEW),
                       'UPDATE',
                       statement_timestamp(),
                       current_setting('var.logged_user'),
                       transaction_timestamp()
                   );
                        
                   RETURN NEW;
               elsif (TG_OP = 'DELETE') then
                   INSERT INTO audit_log (
                       table_name,
                       row_id,
                       old_row_data,
                       new_row_data,
                       dml_type,
                       dml_timestamp,
                       dml_created_by,
                       trx_timestamp
                   )
                   VALUES(
                       TG_TABLE_NAME,
                       OLD.id,
                       to_jsonb(OLD),
                       null,
                       'DELETE',
                       statement_timestamp(),
                       current_setting('var.logged_user'),
                       transaction_timestamp()
                   );
                   
                   RETURN OLD;
               END IF;
            END;
            $body$
            LANGUAGE plpgsql
            """
        );

        executeStatement("""
            CREATE TRIGGER book_audit_trigger
            AFTER INSERT OR UPDATE OR DELETE ON book
            FOR EACH ROW EXECUTE FUNCTION audit_log_trigger_function()
            """
        );

        executeStatement("""
            CREATE TRIGGER author_audit_trigger
            AFTER INSERT OR UPDATE OR DELETE ON author
            FOR EACH ROW EXECUTE FUNCTION audit_log_trigger_function()
            """
        );
    }

    @Test
    public void test() {
        if(!ENABLE_LONG_RUNNING_TESTS) {
            return;
        }
        LoggedUser.logIn("Vlad Mihalcea");

        AtomicInteger auditLogCount = new AtomicInteger();

        doInJPA(entityManager -> {
            setCurrentLoggedUser(entityManager);

            Author author = new Author()
                .setId(1L)
                .setFirstName("Vlad")
                .setLastName("Mihalcea")
                .setCountry("România");

            entityManager.persist(author);

            entityManager.persist(
                new Book()
                    .setId(1L)
                    .setTitle("High-Performance Java Persistence 1st edition")
                    .setPublisher("Amazon")
                    .setPriceInCents(3990)
                    .setAuthor(author)
            );
        });

        doInJPA(entityManager -> {
            List<Tuple> revisions = getPostRevisions(entityManager);

            //Inserting the author
            auditLogCount.incrementAndGet();
            //Inserting the book
            auditLogCount.incrementAndGet();

            assertEquals(auditLogCount.get(), revisions.size());
        });

        doInJPA(entityManager -> {
            setCurrentLoggedUser(entityManager);

            entityManager.find(Author.class, 1L)
                .setTaxTreatyClaiming(true);

            entityManager.find(Book.class, 1L)
                .setPriceInCents(4499);
        });

        doInJPA(entityManager -> {
            List<Tuple> revisions = getPostRevisions(entityManager);

            //Updating the author
            auditLogCount.incrementAndGet();
            //Updating the book
            auditLogCount.incrementAndGet();

            assertEquals(auditLogCount.get(), revisions.size());
        });

        doInJPA(entityManager -> {
            setCurrentLoggedUser(entityManager);

            entityManager.remove(
                entityManager.getReference(Book.class, 1L)
            );
        });

        doInJPA(entityManager -> {
            List<Tuple> revisions = getPostRevisions(entityManager);

            //Deleting the book
            auditLogCount.incrementAndGet();

            assertEquals(auditLogCount.get(), revisions.size());

            List<Tuple> bookRevisions = entityManager.createNativeQuery("""
                SELECT
                    row_id AS id,
                    cast(new_row_data ->> 'price_in_cents' AS int) AS price_in_cents,
                    new_row_data ->> 'publisher' AS publisher,
                    new_row_data ->> 'title' AS title,
                    new_row_data ->> 'author_id' AS author_id,
                    dml_timestamp as version_timestamp
                FROM
                    audit_log
                WHERE
                    table_name = 'book' AND
                    audit_log.row_id = :bookId
                ORDER BY dml_timestamp
			    """, Tuple.class)
            .setParameter("bookId", 1L)
            .getResultList();

            assertEquals(3, bookRevisions.size());
        });
    }

    private void setCurrentLoggedUser(EntityManager entityManager) {
        Session session = entityManager.unwrap(Session.class);
        Dialect dialect = session.getSessionFactory().unwrap(SessionFactoryImplementor.class).getJdbcServices().getDialect();
        String loggedUser = dialect.inlineLiteral(LoggedUser.get());

        session.doWork(connection -> {
            update(
                connection,
                String.format(
                    "SET LOCAL var.logged_user = %s", loggedUser
                )
            );
        });
    }

    private List<Tuple> getPostRevisions(EntityManager entityManager) {
        return entityManager.createNativeQuery("""
            SELECT 
                row_id, 
            	old_row_data,
            	new_row_data,
            	dml_type,
            	dml_timestamp,
            	dml_created_by,
            	trx_timestamp
            FROM audit_log 
            ORDER BY dml_timestamp
            """, Tuple.class)
        .unwrap(org.hibernate.query.NativeQuery.class)
        .getResultList();
    }

    public static class LoggedUser {

        private static final ThreadLocal<String> userHolder = new ThreadLocal<>();

        public static void logIn(String user) {
            userHolder.set(user);
        }

        public static void logOut() {
            userHolder.remove();
        }

        public static String get() {
            return userHolder.get();
        }
    }

    @Entity(name = "Book")
    @Table(name = "book")
    @DynamicUpdate
    public static class Book {

        @Id
        private Long id;

        private String title;

        @ManyToOne(fetch = FetchType.LAZY)
        private Author author;

        @Column(name = "price_in_cents")
        private int priceInCents;

        private String publisher;

        public Long getId() {
            return id;
        }

        public Book setId(Long id) {
            this.id = id;
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
