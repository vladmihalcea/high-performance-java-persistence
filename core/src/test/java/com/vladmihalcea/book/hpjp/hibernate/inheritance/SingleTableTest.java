package com.vladmihalcea.book.hpjp.hibernate.inheritance;

import com.vladmihalcea.book.hpjp.util.AbstractTest;
import org.junit.Test;

import javax.persistence.*;
import java.math.BigDecimal;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.Date;
import java.util.List;

/**
 * <code>SingleTableTest</code> - Single Table Test
 *
 * @author Vlad Mihalcea
 */
public class SingleTableTest extends AbstractTest {

    @Override
    protected Class<?>[] entities() {
        return new Class<?>[]{
            Topic.class,
            Post.class,
            Announcement.class,
        };
    }

    @Test
    public void test() {
        doInJPA(entityManager -> {
            Post post = new Post();
            post.setId(1L);
            post.setOwner("John Doe");
            post.setTitle("First post");
            post.setContent("Hello world!");

            entityManager.persist(post);

            Announcement announcement = new Announcement();
            announcement.setId(2L);
            announcement.setOwner("John Doe");
            announcement.setTitle("Release 1.0.Final");
            announcement.setValidUntil(Timestamp.valueOf(LocalDateTime.now().plusMonths(1)));

            entityManager.persist(announcement);
        });

        doInJPA(entityManager -> {
            List<Topic> topics = entityManager
                .createQuery("select s from Topic s", Topic.class)
                .getResultList();
        });
    }

    @Entity(name = "Topic")
    @Inheritance(strategy = InheritanceType.SINGLE_TABLE)
    public static class Topic {

        @Id
        private Long id;

        private String title;

        private String owner;

        @Temporal(TemporalType.TIMESTAMP)
        private Date createdOn = new Date();

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

        public String getOwner() {
            return owner;
        }

        public void setOwner(String owner) {
            this.owner = owner;
        }

        public Date getCreatedOn() {
            return createdOn;
        }

        public void setCreatedOn(Date createdOn) {
            this.createdOn = createdOn;
        }
    }

    @Entity(name = "Post")
    public static class Post extends Topic {

        private String content;

        public String getContent() {
            return content;
        }

        public void setContent(String content) {
            this.content = content;
        }
    }

    @Entity(name = "Announcement")
    public static class Announcement extends Topic {

        @Temporal(TemporalType.TIMESTAMP)
        private Date validUntil;

        public Date getValidUntil() {
            return validUntil;
        }

        public void setValidUntil(Date validUntil) {
            this.validUntil = validUntil;
        }
    }
}
