package com.vladmihalcea.book.hpjp.hibernate.mapping.embeddable;

import com.vladmihalcea.book.hpjp.util.AbstractTest;
import org.junit.Test;

import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * @author Vlad Mihalcea
 */
public class EmbeddableEntityListenerTest extends AbstractTest {

    @Override
    protected Class<?>[] entities() {
        return new Class<?>[]{
                Post.class,
                Tag.class
        };
    }

    @Test
    public void test() {
        LoggedUser.logIn("Alice");

        doInJPA(entityManager -> {
            Tag jdbc = new Tag();
            jdbc.setName("JDBC");
            entityManager.persist(jdbc);

            Tag hibernate = new Tag();
            hibernate.setName("Hibernate");
            entityManager.persist(hibernate);

            Tag jOOQ = new Tag();
            jOOQ.setName("jOOQ");
            entityManager.persist(jOOQ);
        });

        doInJPA(entityManager -> {
            Post post = new Post();
            post.setId(1L);
            post.setTitle("High-Performance Java Persistence, 1st Edition");

            post.getTags().add(entityManager.find(Tag.class, "JDBC"));
            post.getTags().add(entityManager.find(Tag.class, "Hibernate"));
            post.getTags().add(entityManager.find(Tag.class, "jOOQ"));

            entityManager.persist(post);
        });

        doInJPA(entityManager -> {
            Post post = entityManager.find(Post.class, 1L);

            post.setTitle("High-Performance Java Persistence, 2nd Edition");
        });

        LoggedUser.logOut();
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

    @Embeddable
    public static class Audit {

        @Column(name = "created_on")
        private LocalDateTime createdOn;

        @Column(name = "created_by")
        private String createdBy;

        @Column(name = "updated_on")
        private LocalDateTime updatedOn;

        @Column(name = "updated_by")
        private String updatedBy;

        @PrePersist
        public void prePersist() {
            createdOn = LocalDateTime.now();
            createdBy = LoggedUser.get();
        }

        @PreUpdate
        public void preUpdate() {
            updatedOn = LocalDateTime.now();
            updatedBy = LoggedUser.get();
        }

        public LocalDateTime getCreatedOn() {
            return createdOn;
        }

        public void setCreatedOn(LocalDateTime createdOn) {
            this.createdOn = createdOn;
        }

        public String getCreatedBy() {
            return createdBy;
        }

        public void setCreatedBy(String createdBy) {
            this.createdBy = createdBy;
        }

        public LocalDateTime getUpdatedOn() {
            return updatedOn;
        }

        public void setUpdatedOn(LocalDateTime updatedOn) {
            this.updatedOn = updatedOn;
        }

        public String getUpdatedBy() {
            return updatedBy;
        }

        public void setUpdatedBy(String updatedBy) {
            this.updatedBy = updatedBy;
        }
    }

    @Entity(name = "Post")
    @Table(name = "post")
    public static class Post {

        @Id
        private Long id;

        @Embedded
        private Audit audit = new Audit();

        private String title;

        @ManyToMany
        @JoinTable(name = "post_tag",
                joinColumns = @JoinColumn(name = "post_id"),
                inverseJoinColumns = @JoinColumn(name = "tag_id")
        )
        private List<Tag> tags = new ArrayList<>();

        public Long getId() {
            return id;
        }

        public void setId(Long id) {
            this.id = id;
        }

        public Audit getAudit() {
            return audit;
        }

        public void setAudit(Audit audit) {
            this.audit = audit;
        }

        public String getTitle() {
            return title;
        }

        public void setTitle(String title) {
            this.title = title;
        }

        public List<Tag> getTags() {
            return tags;
        }
    }

    @Entity(name = "Tag")
    @Table(name = "tag")
    public static class Tag {

        @Id
        private String name;

        @Embedded
        private Audit audit = new Audit();

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public Audit getAudit() {
            return audit;
        }

        public void setAudit(Audit audit) {
            this.audit = audit;
        }
    }
}
