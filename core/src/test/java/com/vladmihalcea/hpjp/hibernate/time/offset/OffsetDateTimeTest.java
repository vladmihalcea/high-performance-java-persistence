package com.vladmihalcea.hpjp.hibernate.time.offset;

import com.vladmihalcea.hpjp.util.AbstractTest;
import com.vladmihalcea.hpjp.util.providers.Database;
import jakarta.persistence.*;
import org.hibernate.annotations.TimeZoneColumn;
import org.hibernate.annotations.TimeZoneStorage;
import org.hibernate.annotations.TimeZoneStorageType;
import org.junit.Test;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;

import static org.junit.Assert.assertEquals;

/**
 * @author Vlad Mihalcea
 */
public class OffsetDateTimeTest extends AbstractTest {

    @Override
    protected Class<?>[] entities() {
        return new Class<?>[]{
            Post.class,
            UserAccount.class
        };
    }

    @Override
    protected Database database() {
        return Database.MYSQL;
    }

    @Test
    public void test() {
        OffsetDateTime offsetDateTime = OffsetDateTime.of(
            2024, 2, 29,
            12, 30, 0, 0,
            ZoneOffset.of("+01:00")
        );

        doInJPA(entityManager -> {
            UserAccount user = new UserAccount()
                .setId(1L)
                .setFirstName("Vlad")
                .setLastName("Mihalcea")
                .setSubscribedOn(
                    LocalDate.of(
                        2013, 9, 29
                    )
                );

            Post post = new Post()
                .setId(1L)
                .setTitle("High-Performance Java Persistence")
                .setCreatedBy(user)
                .setPublishedOn(offsetDateTime);

            entityManager.persist(user);

            entityManager.persist(post);
        });

        doInJPA(entityManager -> {
            Post post = entityManager.find(
                Post.class, 1L
            );

            assertEquals(
                offsetDateTime.toInstant(),
                post.getPublishedOn().toInstant()
            );

            UserAccount userAccount = post.getCreatedBy();

            assertEquals(
                LocalDate.of(
                    2013, 9, 29
                ),
                userAccount.getSubscribedOn()
            );
        });
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
        @TimeZoneStorage(TimeZoneStorageType.COLUMN)
        @TimeZoneColumn(name = "published_on_offset")
        private OffsetDateTime publishedOn;

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

        public OffsetDateTime getPublishedOn() {
            return publishedOn;
        }

        public Post setPublishedOn(OffsetDateTime publishedOn) {
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
        private LocalDate subscribedOn;

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

        public LocalDate getSubscribedOn() {
            return subscribedOn;
        }

        public UserAccount setSubscribedOn(LocalDate subscribedOn) {
            this.subscribedOn = subscribedOn;
            return this;
        }
    }
}
