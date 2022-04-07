package com.vladmihalcea.book.hpjp.hibernate.time;

import com.vladmihalcea.book.hpjp.util.AbstractMySQLIntegrationTest;
import org.junit.Test;

import jakarta.persistence.*;
import java.sql.Date;
import java.sql.Timestamp;
import java.text.ParseException;
import java.text.SimpleDateFormat;

import static org.junit.Assert.assertEquals;

/**
 * @author Vlad Mihalcea
 */
public class JavaSqlDateTest extends AbstractMySQLIntegrationTest {

    @Override
    protected Class<?>[] entities() {
        return new Class<?>[]{
            Post.class,
            UserAccount.class
        };
    }

    @Test
    public void test() {
        doInJPA(entityManager -> {
            UserAccount user = new UserAccount()
                .setId(1L)
                .setFirstName("Vlad")
                .setLastName("Mihalcea")
                .setSubscribedOn(
                    parseDate("2013-09-29")
                );

            Post post = new Post()
                .setId(1L)
                .setTitle("High-Performance Java Persistence")
                .setCreatedBy(user)
                .setPublishedOn(
                    parseTimestamp("2020-05-01 12:30:00")
                );

            entityManager.persist(user);

            entityManager.persist(post);
        });

        doInJPA(entityManager -> {
            Post post = entityManager.find(
                Post.class, 1L
            );

            assertEquals(
                parseTimestamp("2020-05-01 12:30:00"),
                post.getPublishedOn()
            );

            UserAccount userAccount = post.getCreatedBy();

            assertEquals(
                parseDate("2013-09-29"),
                userAccount.getSubscribedOn()
            );
        });
    }

    private final SimpleDateFormat DATE_FORMAT = new SimpleDateFormat("yyyy-MM-dd");

    private final SimpleDateFormat DATE_TIME_FORMAT = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

    private java.sql.Date parseDate(String date) {
        try {
            return new Date(DATE_FORMAT.parse(date).getTime());
        } catch (ParseException e) {
            throw new IllegalArgumentException(e);
        }
    }

    private java.sql.Timestamp parseTimestamp(String timestamp) {
        try {
            return new Timestamp(DATE_TIME_FORMAT.parse(timestamp).getTime());
        } catch (ParseException e) {
            throw new IllegalArgumentException(e);
        }
    }

    @Entity(name = "Post")
    @Table(name = "post")
    public static class Post {

        @Id
        private Long id;

        @Column(length = 100)
        private String title;

        @ManyToOne(fetch = FetchType.LAZY)
        @JoinColumn(name = "user_account_id")
        private UserAccount createdBy;

        @Column(name = "published_on")
        private java.sql.Timestamp publishedOn;

        public Long getId() {
            return id;
        }

        public Post setId(Long id) {
            this.id = id;
            return this;
        }

        public String getTitle() {
            return title;
        }

        public Post setTitle(String title) {
            this.title = title;
            return this;
        }

        public UserAccount getCreatedBy() {
            return createdBy;
        }

        public Post setCreatedBy(UserAccount createdBy) {
            this.createdBy = createdBy;
            return this;
        }

        public java.sql.Timestamp getPublishedOn() {
            return publishedOn;
        }

        public Post setPublishedOn(java.sql.Timestamp publishedOn) {
            this.publishedOn = publishedOn;
            return this;
        }
    }

    @Entity(name = "UserAccount")
    @Table(name = "user_account")
    public static class UserAccount {

        @Id
        private Long id;

        @Column(name = "first_name", length = 50)
        private String firstName;

        @Column(name = "last_name", length = 50)
        private String lastName;

        @Column(name = "subscribed_on")
        private java.sql.Date subscribedOn;

        public Long getId() {
            return id;
        }

        public UserAccount setId(Long id) {
            this.id = id;
            return this;
        }

        public String getFirstName() {
            return firstName;
        }

        public UserAccount setFirstName(String firstName) {
            this.firstName = firstName;
            return this;
        }

        public String getLastName() {
            return lastName;
        }

        public UserAccount setLastName(String lastName) {
            this.lastName = lastName;
            return this;
        }

        public java.sql.Date getSubscribedOn() {
            return subscribedOn;
        }

        public UserAccount setSubscribedOn(java.sql.Date subscribedOn) {
            this.subscribedOn = subscribedOn;
            return this;
        }
    }
}
