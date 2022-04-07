package com.vladmihalcea.book.hpjp.hibernate.time;

import com.vladmihalcea.book.hpjp.util.AbstractMySQLIntegrationTest;
import org.hibernate.cfg.AvailableSettings;
import org.junit.Test;

import jakarta.persistence.*;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Properties;

import static org.junit.Assert.assertEquals;

/**
 * @author Vlad Mihalcea
 */
public class JavaUtilDateTest extends AbstractMySQLIntegrationTest {

    @Override
    protected Class<?>[] entities() {
        return new Class<?>[]{
            Post.class,
            UserAccount.class
        };
    }

    @Override
    protected void additionalProperties(Properties properties) {
        properties.setProperty(AvailableSettings.JDBC_TIME_ZONE, "UTC");
    }

    @Test
    public void test() {
        doInJPA(entityManager -> {
            UserAccount user = new UserAccount()
                .setId(1L)
                .setFirstName("Vlad")
                .setLastName("Mihalcea")
                .setSubscribedOn(
                    parseDate("2020-05-01")
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
                parseDate("2020-05-01"),
                userAccount.getSubscribedOn()
            );
        });
    }

    private final SimpleDateFormat DATE_FORMAT = new SimpleDateFormat("yyyy-MM-dd");

    private final SimpleDateFormat DATE_TIME_FORMAT = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

    private java.util.Date parseDate(String date) {
        try {
            return DATE_FORMAT.parse(date);
        } catch (ParseException e) {
            throw new IllegalArgumentException(e);
        }
    }

    private java.util.Date parseTimestamp(String timestamp) {
        try {
            return DATE_TIME_FORMAT.parse(timestamp);
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
        @Temporal(TemporalType.TIMESTAMP)
        private java.util.Date publishedOn;

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

        public java.util.Date getPublishedOn() {
            return publishedOn;
        }

        public Post setPublishedOn(java.util.Date publishedOn) {
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
        @Temporal(TemporalType.DATE)
        private java.util.Date subscribedOn;

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

        public java.util.Date getSubscribedOn() {
            return subscribedOn;
        }

        public UserAccount setSubscribedOn(java.util.Date subscribedOn) {
            this.subscribedOn = subscribedOn;
            return this;
        }
    }
}
