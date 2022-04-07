package com.vladmihalcea.book.hpjp.hibernate.fetching;

import com.vladmihalcea.book.hpjp.util.AbstractTest;
import org.junit.Test;

import jakarta.persistence.*;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Root;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.Date;
import java.util.List;

import static org.junit.Assert.assertEquals;

/**
 * @author Vlad Mihalcea
 */
public class CriteriaAPIEntityTypeJoinedTest extends AbstractTest {

    @Override
    protected Class<?>[] entities() {
        return new Class[] {
                Topic.class,
                Post.class,
                Announcement.class
        };
    }

    @Override
    public void afterInit() {
        doInJPA(entityManager -> {
            Post post = new Post();
            post.setOwner("Vlad");
            post.setTitle("Inheritance");
            post.setContent("Best practices");

            entityManager.persist(post);

            Announcement announcement = new Announcement();
            announcement.setOwner("Vlad");
            announcement.setTitle("Release x.y.z.Final");
            announcement.setValidUntil(Timestamp.valueOf(LocalDateTime.now().plusMonths(1)));

            entityManager.persist(announcement);
        });
    }

    @Test
    public void test() {
        doInJPA(entityManager -> {
            CriteriaBuilder builder = entityManager.getCriteriaBuilder();

            CriteriaQuery<Topic> criteria = builder.createQuery(Topic.class);
            Root<Topic> root = criteria.from(Topic.class);

            criteria.where(
                builder.equal(root.get("owner"), "Vlad")
            );

            List<Topic> topics = entityManager
            .createQuery(criteria)
            .getResultList();

            assertEquals(2, topics.size());
        });

        doInJPA(entityManager -> {
            CriteriaBuilder builder = entityManager.getCriteriaBuilder();

            CriteriaQuery<Post> criteria = builder.createQuery(Post.class);
            Root<Post> root = criteria.from(Post.class);

            criteria.where(
                builder.equal(root.get("owner"), "Vlad")
            );

            List<Post> posts = entityManager
            .createQuery(criteria)
            .getResultList();

            assertEquals(1, posts.size());
        });

        doInJPA(entityManager -> {
            Class<? extends Topic> sublcass = Post.class;

            CriteriaBuilder builder = entityManager.getCriteriaBuilder();

            CriteriaQuery<Topic> criteria = builder.createQuery(Topic.class);
            Root<Topic> root = criteria.from(Topic.class);

            criteria.where(
                builder.and(
                    builder.equal(root.get("owner"), "Vlad"),
                    builder.equal(root.type(), sublcass)
                )
            );

            List<Topic> topics = entityManager
            .createQuery(criteria)
            .getResultList();

            assertEquals(1, topics.size());
        });
    }

    @Entity(name = "Topic")
    @Table(name = "topic")
    @Inheritance(strategy = InheritanceType.JOINED)
    @DiscriminatorColumn
    @DiscriminatorValue("0")
    public static class Topic {

        @Id
        @GeneratedValue
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
    @DiscriminatorValue("1")
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
    @DiscriminatorValue("2")
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
