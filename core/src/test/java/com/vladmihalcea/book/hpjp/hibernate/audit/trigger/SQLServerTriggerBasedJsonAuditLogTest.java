package com.vladmihalcea.book.hpjp.hibernate.audit.trigger;

import com.vladmihalcea.book.hpjp.util.AbstractTest;
import com.vladmihalcea.book.hpjp.util.providers.Database;
import com.vladmihalcea.book.hpjp.util.ReflectionUtils;
import com.vladmihalcea.hibernate.type.json.JsonNodeStringType;
import org.hibernate.Session;
import org.hibernate.annotations.DynamicUpdate;
import org.hibernate.dialect.Dialect;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.usertype.UserType;
import org.junit.Test;

import jakarta.persistence.*;
import java.util.List;
import java.util.concurrent.TimeUnit;

import static org.junit.Assert.assertEquals;

/**
 * @author Vlad Mihalcea
 */
public class SQLServerTriggerBasedJsonAuditLogTest extends AbstractTest {

    @Override
    protected Class<?>[] entities() {
        return new Class[]{
            Book.class
        };
    }

    @Override
    protected Database database() {
        return Database.SQLSERVER;
    }

    @Override
    protected List<UserType<?>> additionalTypes() {
        return List.of(JsonNodeStringType.INSTANCE);
    }

    @Override
    protected void afterInit() {
        executeStatement("DROP TABLE BookAuditLog");
        executeStatement("""
            CREATE TABLE BookAuditLog (
                BookId bigint NOT NULL, 
            	OldRowData nvarchar(1000) CHECK(ISJSON(OldRowData) = 1),
            	NewRowData nvarchar(1000) CHECK(ISJSON(NewRowData) = 1),
            	DmlType varchar(10) NOT NULL CHECK (DmlType IN ('INSERT', 'UPDATE', 'DELETE')),
            	DmlTimestamp datetime NOT NULL,
            	DmlCreatedBy varchar(255) NOT NULL,
            	TrxTimestamp datetime NOT NULL,
            	PRIMARY KEY (BookId, DmlType, DmlTimestamp)
            ) 
            """
        );

        executeStatement("""       
            CREATE TRIGGER TR_Book_Insert_AuditLog ON Book
            FOR INSERT AS 
            BEGIN
                DECLARE @loggedUser varchar(255)
                SELECT @loggedUser = cast(SESSION_CONTEXT(N'loggedUser') as varchar(255))
                
                DECLARE @transactionTimestamp datetime = SYSUTCdatetime()
                
                INSERT INTO BookAuditLog (
                    BookId,
                    OldRowData,
                    NewRowData,
                    DmlType,
                    DmlTimestamp,
                    DmlCreatedBy,
                    TrxTimestamp
                )
                VALUES(
                    (SELECT id FROM Inserted),
                    null,
                    (SELECT * FROM Inserted FOR JSON PATH, WITHOUT_ARRAY_WRAPPER),
                    'INSERT',
                    CURRENT_TIMESTAMP,
                    @loggedUser,
                    @transactionTimestamp
                );
            END
            """
        );

        executeStatement("""
            CREATE TRIGGER TR_Book_Update_AuditLog ON Book
            FOR UPDATE AS 
            BEGIN
                DECLARE @loggedUser varchar(255)
                SELECT @loggedUser = cast(SESSION_CONTEXT(N'loggedUser') as varchar(255))
                
                DECLARE @transactionTimestamp datetime = SYSUTCdatetime()
                
                INSERT INTO BookAuditLog (
                    BookId,
                    OldRowData,
                    NewRowData,
                    DmlType,
                    DmlTimestamp,
                    DmlCreatedBy,
                    TrxTimestamp
                )
                VALUES(
                    (SELECT id FROM Inserted),
                    (SELECT * FROM Deleted FOR JSON PATH, WITHOUT_ARRAY_WRAPPER),
                    (SELECT * FROM Inserted FOR JSON PATH, WITHOUT_ARRAY_WRAPPER),
                    'UPDATE',
                    CURRENT_TIMESTAMP,
                    @loggedUser,
                    @transactionTimestamp
                );
            END
            """
        );

        executeStatement("""
            CREATE TRIGGER TR_Book_Delete_AuditLog ON Book
            FOR DELETE AS 
            BEGIN
                DECLARE @loggedUser varchar(255)
                SELECT @loggedUser = cast(SESSION_CONTEXT(N'loggedUser') as varchar(255))
                
                DECLARE @transactionTimestamp datetime = SYSUTCdatetime()
                
                INSERT INTO BookAuditLog (
                    BookId,
                    OldRowData,
                    NewRowData,
                    DmlType,
                    DmlTimestamp,
                    DmlCreatedBy,
                    TrxTimestamp
                )
                VALUES(
                    (SELECT id FROM Deleted),
                    (SELECT * FROM Deleted FOR JSON PATH, WITHOUT_ARRAY_WRAPPER),
                    null,
                    'DELETE',
                    CURRENT_TIMESTAMP,
                    @loggedUser,
                    @transactionTimestamp
                );
            END
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

            sleep(TimeUnit.SECONDS.toMillis(1));
        });

        doInJPA(entityManager -> {
            List<Tuple> revisions = getPostRevisions(entityManager);

            assertEquals(1, revisions.size());

        });

        doInJPA(entityManager -> {
            setCurrentLoggedUser(entityManager);

            Book book = entityManager.find(Book.class, 1L)
                .setPriceInCents(4499);

            sleep(TimeUnit.SECONDS.toMillis(1));
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

            sleep(TimeUnit.SECONDS.toMillis(1));
        });

        doInJPA(entityManager -> {
            List<Tuple> revisions = getPostRevisions(entityManager);

            assertEquals(3, revisions.size());

            List<Tuple> bookRevisions = entityManager.createNativeQuery("""
                SELECT
                   BookAuditLog.DmlTimestamp as VersionTimestamp,
                   r.*
                FROM
                   BookAuditLog
                OUTER APPLY
                   OPENJSON (
                     JSON_QUERY(
                        NewRowData,
                        '$'
                     )
                   )
                   WITH (
                      title varchar(255) '$.Title',
                      author varchar(255) '$.Author',
                      price_in_cents bigint '$.PriceInCents',
                      publisher varchar(255) '$.Publisher'
                   ) AS r
                WHERE
                 BookAuditLog.BookId = :bookId
                ORDER BY VersionTimestamp
			    """, Tuple.class)
                .setParameter("bookId", 1L)
                .getResultList();

            assertEquals(3, bookRevisions.size());
        });
    }

    private void setCurrentLoggedUser(EntityManager entityManager) {
        Session session = entityManager.unwrap(Session.class);
        Dialect dialect = session.getSessionFactory().unwrap(SessionFactoryImplementor.class).getJdbcServices().getDialect();
        String loggedUser = ReflectionUtils.invokeMethod(
            dialect,
            "inlineLiteral",
            LoggedUser.get()
        );

        session.doWork(connection -> {
            update(
                connection,
                String.format(
                    "EXEC sys.sp_set_session_context @key = N'loggedUser', @value = N%s, @read_only = 1", loggedUser
                )
            );
        });
    }

    private List<Tuple> getPostRevisions(EntityManager entityManager) {
        return entityManager.createNativeQuery("""
            SELECT *
            FROM BookAuditLog
            ORDER BY DmlTimestamp
            """, Tuple.class)
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
    @Table(name = "Book")
    @DynamicUpdate
    public static class Book {

        @Id
        @Column(name = "Id")
        private Long id;

        @Column(name = "Title")
        private String title;

        @Column(name = "Author")
        private String author;

        @Column(name = "PriceInCents")
        private int priceInCents;

        @Column(name = "Publisher")
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
