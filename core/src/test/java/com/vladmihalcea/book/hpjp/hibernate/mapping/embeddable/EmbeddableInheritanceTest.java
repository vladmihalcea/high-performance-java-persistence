package com.vladmihalcea.book.hpjp.hibernate.mapping.embeddable;

import com.vladmihalcea.book.hpjp.util.AbstractTest;
import org.hibernate.annotations.GenericGenerator;
import org.junit.Test;

import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * @author Vlad Mihalcea
 */
public class EmbeddableInheritanceTest extends AbstractTest {

    @Override
    protected Class<?>[] entities() {
        return new Class<?>[]{
            Post.class,
            PostDetails.class,
            PostComment.class,
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

        byte[] imageBytes = new byte[] {1, 2, 3, 4, 5, 6, 7, 8, 9};

        doInJPA(entityManager -> {
            Post post = new Post();
            post.setId(1L);
            post.setTitle("High-Performance Java Persistence, 1st Edition");

            PostDetails details = new PostDetails();
            details.setImage(imageBytes);

            post.setDetails(details);

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

    public static class AuditListener {

        @PrePersist
        public void setCreatedOn(Auditable auditable) {
            Audit audit = auditable.getAudit();

            if(audit == null) {
                audit = new Audit();
                auditable.setAudit(audit);
            }

            audit.setCreatedOn(LocalDateTime.now());
            audit.setCreatedBy(LoggedUser.get());
        }

        @PreUpdate
        public void setUpdatedOn(Auditable auditable) {
            Audit audit = auditable.getAudit();

            audit.setUpdatedOn(LocalDateTime.now());
            audit.setUpdatedBy(LoggedUser.get());
        }
    }

    public interface Auditable {

        Audit getAudit();

        void setAudit(Audit audit);
    }

    @MappedSuperclass
    public static class BaseAudit {

        @Column(name = "created_on")
        private LocalDateTime createdOn;

        @Column(name = "created_by")
        private String createdBy;

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
    }

    @Embeddable
    public static class Audit extends BaseAudit {

        @Column(name = "updated_on")
        private LocalDateTime updatedOn;

        @Column(name = "updated_by")
        private String updatedBy;

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
    @EntityListeners(AuditListener.class)
    public static class Post implements Auditable {

        @Id
        private Long id;

        @Embedded
        private Audit audit;

        private String title;

        @OneToMany(cascade = CascadeType.ALL, mappedBy = "post",
                orphanRemoval = true)
        private List<PostComment> comments = new ArrayList<>();

        @OneToOne(cascade = CascadeType.ALL, mappedBy = "post",
                orphanRemoval = true, fetch = FetchType.LAZY)
        private PostDetails details;

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

        public List<PostComment> getComments() {
            return comments;
        }

        public PostDetails getDetails() {
            return details;
        }

        public void setDetails(PostDetails details) {
            this.details = details;
            details.setPost(this);
        }

        public List<Tag> getTags() {
            return tags;
        }

        public void addComment(PostComment comment) {
            comments.add(comment);
            comment.setPost(this);
        }

        public void addDetails(PostDetails details) {
            this.details = details;
            details.setPost(this);
        }

        public void removeDetails() {
            this.details.setPost(null);
            this.details = null;
        }
    }

    @Entity(name = "PostDetails")
    @Table(name = "post_details")
    @EntityListeners(AuditListener.class)
    public static class PostDetails implements Auditable {

        @Id
        private Long id;

        @Embedded
        private Audit audit;

        @OneToOne(fetch = FetchType.LAZY)
        @MapsId
        private Post post;

        @Lob
        private byte[] image;

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

        public Post getPost() {
            return post;
        }

        public void setPost(Post post) {
            this.post = post;
        }

        public byte[] getImage() {
            return image;
        }

        public void setImage(byte[] image) {
            this.image = image;
        }
    }

    @Entity(name = "PostComment")
    @Table(name = "post_comment")
    @EntityListeners(AuditListener.class)
    public static class PostComment implements Auditable {

        @Id
        @GeneratedValue(generator = "native")
        @GenericGenerator(name = "native", strategy = "native")
        private Long id;

        @Embedded
        private Audit audit;

        @ManyToOne
        private Post post;

        private String review;

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

        public Post getPost() {
            return post;
        }

        public void setPost(Post post) {
            this.post = post;
        }

        public String getReview() {
            return review;
        }

        public void setReview(String review) {
            this.review = review;
        }
    }

    @Entity(name = "Tag")
    @Table(name = "tag")
    @EntityListeners(AuditListener.class)
    public static class Tag implements Auditable {

        @Id
        private String name;

        @Embedded
        private Audit audit;

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
