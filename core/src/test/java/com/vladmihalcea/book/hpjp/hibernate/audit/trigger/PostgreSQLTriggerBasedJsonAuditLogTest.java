package com.vladmihalcea.book.hpjp.hibernate.audit.trigger;

import com.fasterxml.jackson.databind.JsonNode;
import com.vladmihalcea.book.hpjp.util.AbstractTest;
import com.vladmihalcea.book.hpjp.util.providers.Database;
import com.vladmihalcea.hibernate.type.json.JsonBinaryType;
import com.vladmihalcea.hibernate.type.util.ReflectionUtils;
import org.hibernate.Session;
import org.hibernate.annotations.DynamicUpdate;
import org.hibernate.dialect.Dialect;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.type.LongType;
import org.hibernate.type.StringType;
import org.hibernate.type.TimestampType;
import org.junit.Test;

import javax.persistence.*;
import java.util.List;

import static org.junit.Assert.assertEquals;

/**
 * @author Vlad Mihalcea
 */
public class PostgreSQLTriggerBasedJsonAuditLogTest extends AbstractTest {

    @Override
    protected Class<?>[] entities() {
        return new Class[]{
            Book.class
        };
    }

    @Override
    protected Database database() {
        return Database.POSTGRESQL;
    }

    @Override
    protected void afterInit() {
        ddl("DROP TYPE dml_type CASCADE");
        ddl("CREATE TYPE dml_type AS ENUM ('INSERT', 'UPDATE', 'DELETE')");

        ddl("DROP TABLE IF EXISTS book_audit_log CASCADE");
        ddl("""
            CREATE TABLE IF NOT EXISTS book_audit_log (
                book_id bigint NOT NULL,
            	old_row_data jsonb,
            	new_row_data jsonb,
            	dml_type dml_type NOT NULL,
            	dml_timestamp timestamp NOT NULL,
            	dml_created_by varchar(255) NOT NULL,
            	trx_timestamp timestamp NOT NULL,
            	PRIMARY KEY (book_id, dml_type, dml_timestamp)
            ) 
            """
        );

        ddl("DROP FUNCTION IF EXISTS book_audit_trigger_func cascade");

        ddl("""          
            CREATE OR REPLACE FUNCTION book_audit_trigger_func()
            RETURNS trigger AS $body$
            BEGIN
               if (TG_OP = 'INSERT') then
                   INSERT INTO book_audit_log (
                       book_id,
                       old_row_data,
                       new_row_data,
                       dml_type,
                       dml_timestamp,
                       dml_created_by,
                       trx_timestamp
                   )
                   VALUES(
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
                   INSERT INTO book_audit_log (
                       book_id,
                       old_row_data,
                       new_row_data,
                       dml_type,
                       dml_timestamp,
                       dml_created_by,
                       trx_timestamp
                   )
                   VALUES(
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
                   INSERT INTO book_audit_log (
                       book_id,
                       old_row_data,
                       new_row_data,
                       dml_type,
                       dml_timestamp,
                       dml_created_by,
                       trx_timestamp
                   )
                   VALUES(
                       OLD.id,
                       to_jsonb(OLD),
                       null,
                       'DELETE',
                       statement_timestamp(),
                       current_setting('var.logged_user'),
                       transaction_timestamp()
                   );
                   
                   RETURN OLD;
               end if;
                
            END;
            $body$
            LANGUAGE plpgsql
            """
        );

        ddl("""
            CREATE TRIGGER book_audit_trigger
            AFTER INSERT OR UPDATE OR DELETE ON book
            FOR EACH ROW EXECUTE FUNCTION book_audit_trigger_func();
            """
        );
    }

    @Test
    public void test() {
        LoggedUser.logIn("Vlad Mihalcea");

        doInJPA(entityManager -> {
            setCurrentLoggedUser(entityManager);

            entityManager.persist(
                new Book()
                    .setId(1L)
                    .setTitle("High-Performance Java Persistence 1st edition")
                    .setPublisher("Amazon")
                    .setPriceInCents(3990)
                    .setAuthor("Vlad Mihalcea")
            );
        });

        doInJPA(entityManager -> {
            List<Tuple> revisions = getPostRevisions(entityManager);

            assertEquals(1, revisions.size());
        });

        doInJPA(entityManager -> {
            setCurrentLoggedUser(entityManager);

            Book book = entityManager.find(Book.class, 1L)
                .setPriceInCents(4499);
        });

        doInJPA(entityManager -> {
            List<Tuple> revisions = getPostRevisions(entityManager);

            assertEquals(2, revisions.size());
        });

        doInJPA(entityManager -> {
            setCurrentLoggedUser(entityManager);

            entityManager.remove(
                entityManager.getReference(Book.class, 1L)
            );
        });

        doInJPA(entityManager -> {
            List<Tuple> revisions = getPostRevisions(entityManager);

            assertEquals(3, revisions.size());
        });
    }

    private void setCurrentLoggedUser(EntityManager entityManager) {
        Session session = entityManager.unwrap(Session.class);
        Dialect dialect = session.getSessionFactory().unwrap(SessionFactoryImplementor.class).getJdbcServices().getDialect();
        String loggedUser = ReflectionUtils.invokeMethod(
            dialect,
            "escapeLiteral",
            LoggedUser.get()
        );

        session.doWork(connection -> {
            update(
                connection,
                String.format(
                    "SET LOCAL var.logged_user = '%s'", loggedUser
                )
            );
        });
    }

    private List<Tuple> getPostRevisions(EntityManager entityManager) {
        return entityManager.createNativeQuery("""
            SELECT 
                book_id, 
            	old_row_data,
            	new_row_data,
            	dml_type,
            	dml_timestamp,
            	dml_created_by,
            	trx_timestamp
            FROM book_audit_log 
            ORDER BY dml_timestamp
            """, Tuple.class)
        .unwrap(org.hibernate.query.NativeQuery.class)
        .addScalar("book_id", LongType.INSTANCE)
        .addScalar("old_row_data", new JsonBinaryType(JsonNode.class))
        .addScalar("new_row_data", new JsonBinaryType(JsonNode.class))
        .addScalar("dml_type", StringType.INSTANCE)
        .addScalar("dml_timestamp", TimestampType.INSTANCE)
        .addScalar("dml_created_by", StringType.INSTANCE)
        .addScalar("trx_timestamp", TimestampType.INSTANCE)
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

        private String author;

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

        public String getAuthor() {
            return author;
        }

        public Book setAuthor(String author) {
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
}
