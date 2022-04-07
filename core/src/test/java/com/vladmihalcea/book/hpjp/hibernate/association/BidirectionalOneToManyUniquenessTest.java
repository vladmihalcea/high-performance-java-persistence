package com.vladmihalcea.book.hpjp.hibernate.association;

import com.vladmihalcea.book.hpjp.util.AbstractTest;
import org.junit.Test;

import jakarta.persistence.*;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Objects;

import static org.junit.Assert.assertEquals;

/**
 * @author Vlad Mihalcea
 */
public class BidirectionalOneToManyUniquenessTest extends AbstractTest {

    @Override
    protected Class<?>[] entities() {
        return new Class<?>[]{
                Post.class,
                PostComment.class,
        };
    }

    @Test
    public void testLifecycle() {
        final PostComment comment = doInJPA(entityManager -> {
            Post post = new Post("First post");
            entityManager.persist(post);

            PostComment comment1 = new PostComment("My first review");
            comment1.setCreatedBy("Vlad");
            post.addComment(comment1);
            PostComment comment2 = new PostComment("My second review");
            comment2.setCreatedBy("Vlad");
            post.addComment(comment2);

            entityManager.persist(post);
            entityManager.flush();

            return comment1;
        });

        doInJPA(entityManager -> {
            Post post = entityManager.find(Post.class, 1L);

            post.removeComment(comment);
            assertEquals(1, post.getComments().size());
        });
    }

    @Test
    public void testShuffle() {
        final PostComment comment = doInJPA(entityManager -> {
            Post post = new Post("First post");
            entityManager.persist(post);

            PostComment comment1 = new PostComment("My first review");
            comment1.setCreatedBy("Vlad");
            post.addComment(comment1);
            PostComment comment2 = new PostComment("My second review");
            comment2.setCreatedBy("Vlad");
            post.addComment(comment2);

            entityManager.persist(post);
            entityManager.flush();

            return comment1;
        });

        doInJPA(entityManager -> {
            Post post = entityManager.find(Post.class, 1L);

            post.removeComment(comment);
            assertEquals(1, post.getComments().size());
        });
    }

    @Entity(name = "Post")
    @Table(name = "post")
    public static class Post {

        @Id
        @GeneratedValue
        private Long id;

        private String title;

        @OneToMany(mappedBy = "post", cascade = CascadeType.ALL, orphanRemoval = true)
        private List<PostComment> comments = new ArrayList<>();

        public Post() {
        }

        public Post(String title) {
            this.title = title;
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

        public void addComment(PostComment comment) {
            comments.add(comment);
            comment.setPost(this);
        }

        public void removeComment(PostComment comment) {
            comments.remove(comment);
            comment.setPost(null);
        }
    }

    @Entity(name = "PostComment")
    @Table(name = "post_comment")
    public static class PostComment {

        @Id
        @GeneratedValue
        private Long id;

        private String review;

        private String createdBy;

        @Temporal(TemporalType.TIMESTAMP)
        private Date createdOn = new Date();

        @ManyToOne
        @JoinColumn(name = "post_id")
        private Post post;

        public PostComment() {
        }

        public PostComment(String review) {
            this.review = review;
        }

        public Long getId() {
            return id;
        }

        public void setId(Long id) {
            this.id = id;
        }

        public String getReview() {
            return review;
        }

        public void setReview(String review) {
            this.review = review;
        }

        public String getCreatedBy() {
            return createdBy;
        }

        public void setCreatedBy(String createdBy) {
            this.createdBy = createdBy;
        }

        public Date getCreatedOn() {
            return createdOn;
        }

        public void setCreatedOn(Date createdOn) {
            this.createdOn = createdOn;
        }

        public Post getPost() {
            return post;
        }

        public void setPost(Post post) {
            this.post = post;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            PostComment that = (PostComment) o;
            return Objects.equals(createdBy, that.createdBy) &&
                    Objects.equals(createdOn, that.createdOn);
        }

        @Override
        public int hashCode() {
            return Objects.hash(createdBy, createdOn);
        }
    }
}
