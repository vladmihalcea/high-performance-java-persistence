package com.vladmihalcea.book.hpjp.hibernate.pc;

import com.vladmihalcea.book.hpjp.hibernate.identifier.Identifiable;
import com.vladmihalcea.book.hpjp.util.AbstractTest;
import org.hibernate.Session;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.GenericGenerator;
import org.junit.Test;

import jakarta.persistence.*;
import java.sql.Statement;
import java.util.*;

public class CloneTest extends AbstractTest {

    @Override
    protected Class<?>[] entities() {
        return new Class<?>[] {
                Post.class,
                PostDetails.class,
                PostComment.class,
                Tag.class
        };
    }

    @Override
    protected void afterInit() {
        doInJPA(entityManager -> {
            Tag java = new Tag();
            java.setName("Java");

            entityManager.persist(java);

            Tag jdbc = new Tag();
            jdbc.setName("JDBC");

            entityManager.persist(jdbc);

            Tag jpa = new Tag();
            jpa.setName("JPA");

            entityManager.persist(jpa);

            Tag jooq = new Tag();
            jooq.setName("jOOQ");

            entityManager.persist(jooq);
        });

        doInJPA(entityManager -> {
            Post post = new Post();
            post.setTitle("High-Performance Java Persistence, 1st edition");

            PostDetails details = new PostDetails();
            details.setCreatedBy("Vlad Mihalcea");
            post.addDetails(details);

            post.getTags().add(entityManager.getReference(Tag.class, "Java"));
            post.getTags().add(entityManager.getReference(Tag.class, "JDBC"));
            post.getTags().add(entityManager.getReference(Tag.class, "JPA"));
            post.getTags().add(entityManager.getReference(Tag.class, "jOOQ"));

            PostComment comment1 = new PostComment();
            comment1.setReview("This book is a big one");
            post.addComment(comment1);

            PostComment comment2 = new PostComment();
            comment2.setReview("5 stars");
            post.addComment(comment2);

            entityManager.persist(post);
        });
    }

    @Test
    public void testClone() {
        doInJPA(entityManager -> {
            Post post = entityManager.createQuery(
                "select p " +
                "from Post p " +
                "join fetch p.details " +
                "join fetch p.tags " +
                "where p.title = :title", Post.class)
            .setParameter("title", "High-Performance Java Persistence, 1st edition")
            .getSingleResult();

            Post postClone = new Post(post);
            postClone.setTitle(postClone.getTitle().replace("1st", "2nd"));
            entityManager.persist(postClone);
        });
    }

    @Entity(name = "Post")
    @Table(name = "post")
    public static class Post {

        @Id
        @GeneratedValue
        private Long id;

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
        private Set<Tag> tags = new HashSet<>();

        /**
         * Needed by Hibernate when hydrating the entity
         * from the JDBC ResultSet
         */
        private Post() {}

        public Post(Post post) {
            this.title = post.title;

            addDetails(new PostDetails(post.details));

            tags.addAll(post.getTags());
        }

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

        public List<PostComment> getComments() {
            return comments;
        }

        public PostDetails getDetails() {
            return details;
        }

        public Set<Tag> getTags() {
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
    public static class PostDetails {

        @Id
        private Long id;

        @Column(name = "created_on")
        @CreationTimestamp
        private Date createdOn;

        @Column(name = "created_by")
        private String createdBy;

        @OneToOne(fetch = FetchType.LAZY)
        @MapsId
        private Post post;

        /**
         * Needed by Hibernate when hydrating the entity
         * from the JDBC ResultSet
         */
        private PostDetails() {
        }

        public PostDetails(PostDetails details) {
            this.createdBy = details.createdBy;
        }

        public Long getId() {
            return id;
        }

        public void setId(Long id) {
            this.id = id;
        }

        public Post getPost() {
            return post;
        }

        public void setPost(Post post) {
            this.post = post;
        }

        public Date getCreatedOn() {
            return createdOn;
        }

        public void setCreatedOn(Date createdOn) {
            this.createdOn = createdOn;
        }

        public String getCreatedBy() {
            return createdBy;
        }

        public void setCreatedBy(String createdBy) {
            this.createdBy = createdBy;
        }
    }

    @Entity(name = "PostComment")
    @Table(name = "post_comment")
    public static class PostComment {

        @Id
        @GeneratedValue
        private Long id;

        @ManyToOne(fetch = FetchType.LAZY)
        private Post post;

        private String review;

        public Long getId() {
            return id;
        }

        public void setId(Long id) {
            this.id = id;
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
    public static class Tag {

        @Id
        private String name;

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }
    }

}
