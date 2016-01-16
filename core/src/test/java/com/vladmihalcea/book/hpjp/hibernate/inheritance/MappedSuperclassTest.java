package com.vladmihalcea.book.hpjp.hibernate.inheritance;

import com.vladmihalcea.book.hpjp.util.AbstractTest;
import org.junit.Test;

import javax.persistence.*;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.Date;

/**
 * <code>MappedSuperclassTest</code> - MappedSuperclass Test
 *
 * @author Vlad Mihalcea
 */
public class MappedSuperclassTest extends AbstractTest {

    @Override
    protected Class<?>[] entities() {
        return new Class<?>[] {
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
            announcement.setId(1L);
            announcement.setOwner("John Doe");
            announcement.setTitle("Release 1.0.Final");
            announcement.setValidUntil(Timestamp.valueOf(LocalDateTime.now().plusMonths(1)));

            entityManager.persist(announcement);
        });
    }

    @MappedSuperclass
    public static abstract class Topic {

        private String title;

        private String owner;

        @Temporal(TemporalType.TIMESTAMP)
        private Date createdOn = new Date();

        public abstract Long getId();

        public abstract void setId(Long id);

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

        @Id
        private Long id;

        private String content;

        @Override
        public Long getId() {
            return id;
        }

        @Override
        public void setId(Long id) {
            this.id = id;
        }

        public String getContent() {
            return content;
        }

        public void setContent(String content) {
            this.content = content;
        }
    }

    @Entity(name = "Announcement")
    public static class Announcement extends Topic {

        @Id
        private Long id;

        @Temporal(TemporalType.TIMESTAMP)
        private Date validUntil;

        @Override
        public Long getId() {
            return id;
        }

        @Override
        public void setId(Long id) {
            this.id = id;
        }

        public Date getValidUntil() {
            return validUntil;
        }

        public void setValidUntil(Date validUntil) {
            this.validUntil = validUntil;
        }
    }
}
