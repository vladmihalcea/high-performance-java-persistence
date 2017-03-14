package com.vladmihalcea.book.hpjp.hibernate.time;

import com.vladmihalcea.book.hpjp.util.AbstractPostgreSQLIntegrationTest;
import org.hibernate.Session;
import org.junit.Test;

import java.sql.ResultSet;
import java.sql.Statement;
import java.sql.Timestamp;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.TimeZone;

import static org.junit.Assert.assertEquals;

/**
 * @author Vlad Mihalcea
 */
public class DefaultPostgreSQLTimestampTest extends AbstractPostgreSQLIntegrationTest {

    @Override
    protected Class<?>[] entities() {
        return new Class<?>[]{
                Book.class
        };
    }

    @Test
    public void test() {
        TimeZone defaultTimeZone = TimeZone.getDefault();
        try {
            TimeZone.setDefault(TimeZone.getTimeZone("US/Hawaii"));
            doInJPA(entityManager -> {
                Book book = new Book();
                book.setId(1L);
                book.setTitle("High-Performance Java Persistence");
                book.setCreatedBy("Vlad Mihalcea");
                book.setCreatedOn(new Timestamp(ZonedDateTime.of(2016, 8, 25, 11, 23, 46, 0, ZoneId.of("UTC")).toInstant().toEpochMilli()));

                assertEquals(1472124226000L, book.getCreatedOn().getTime());
                entityManager.persist(book);
            });
            doInJPA(entityManager -> {
                Session session = entityManager.unwrap(Session.class);
                session.doWork(connection -> {
                    try (Statement st = connection.createStatement()) {
                        try (ResultSet rs = st.executeQuery(
                                "SELECT TO_CHAR(created_on, 'YYYY-MM-DD HH24:MI:SS') " +
                                "FROM book")) {
                            while (rs.next()) {
                                String timestamp = rs.getString(1);
                                assertEquals(expectedServerTimestamp(), timestamp);
                            }
                        }
                    }
                });
            });
            doInJPA(entityManager -> {
                Book book = entityManager.find(Book.class, 1L);
                assertEquals(1472124226000L, book.getCreatedOn().getTime());
            });
        } finally {
            TimeZone.setDefault(defaultTimeZone);
        }
    }

    protected String expectedServerTimestamp() {
        return "2016-08-25 01:23:46";
    }
}
