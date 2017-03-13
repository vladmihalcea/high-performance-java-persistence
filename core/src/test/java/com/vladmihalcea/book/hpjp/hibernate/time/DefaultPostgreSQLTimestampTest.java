package com.vladmihalcea.book.hpjp.hibernate.time;

import com.vladmihalcea.book.hpjp.util.AbstractPostgreSQLIntegrationTest;
import org.hibernate.Session;
import org.junit.Test;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Table;
import java.sql.ResultSet;
import java.sql.Statement;
import java.sql.Timestamp;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.Date;
import java.util.TimeZone;

import static org.junit.Assert.assertEquals;

/**
 * @author Vlad Mihalcea
 */
public class DefaultPostgreSQLTimestampTest extends AbstractPostgreSQLIntegrationTest {

    @Override
    protected Class<?>[] entities() {
        return new Class<?>[]{
                Post.class
        };
    }

    @Test
    public void test() {
        TimeZone defaultTimeZone = TimeZone.getDefault();
        try {
            TimeZone.setDefault(TimeZone.getTimeZone("US/Hawaii"));
            doInJPA(entityManager -> {
                Post post = new Post();
                post.setId(1L);
                post.setTitle("High-Performance Java Persistence");
                post.setCreatedBy("Vlad Mihalcea");
                post.setCreatedOn(new Timestamp(ZonedDateTime.of(2016, 8, 25, 11, 23, 46, 0, ZoneId.of("UTC")).toInstant().toEpochMilli()));

                assertEquals(1472124226000L, post.getCreatedOn().getTime());
                entityManager.persist(post);
            });
            doInJPA(entityManager -> {
                Session session = entityManager.unwrap(Session.class);
                session.doWork(connection -> {
                    try (Statement st = connection.createStatement()) {
                        try (ResultSet rs = st.executeQuery(
                                "SELECT TO_CHAR(created_on, 'YYYY-MM-DD HH24:MI:SS') " +
                                "FROM post")) {
                            while (rs.next()) {
                                String timestamp = rs.getString(1);
                                assertEquals(expectedServerTimestamp(), timestamp);
                            }
                        }
                    }
                });
            });
            doInJPA(entityManager -> {
                Post post = entityManager.find(Post.class, 1L);
                assertEquals(Timestamp.valueOf(expectedClientTimestamp()), post.getCreatedOn());
            });
        } finally {
            TimeZone.setDefault(defaultTimeZone);
        }
    }

    protected String expectedServerTimestamp() {
        return "2016-08-25 01:23:46";
    }

    protected String expectedClientTimestamp() {
        return "2016-08-25 01:23:46";
    }

    @Entity(name = "Post")
    @Table(name = "post")
    public static class Post {

        @Id
        private Long id;

        private String title;

        @Column(name = "created_on")
        private Timestamp createdOn;

        @Column(name = "created_by")
        private String createdBy;

        public Long getId() {
            return id;
        }

        public void setId(Long id) {
            this.id = id;
        }

        public String getTitle() {
            return title;
        }

        public void setTitle(String title) {
            this.title = title;
        }

        public Date getCreatedOn() {
            return createdOn;
        }

        public void setCreatedOn(Timestamp createdOn) {
            this.createdOn = createdOn;
        }

        public String getCreatedBy() {
            return createdBy;
        }

        public void setCreatedBy(String createdBy) {
            this.createdBy = createdBy;
        }
    }
}
