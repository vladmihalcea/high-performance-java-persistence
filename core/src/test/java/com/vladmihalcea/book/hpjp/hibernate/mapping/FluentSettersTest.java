package com.vladmihalcea.book.hpjp.hibernate.mapping;

import com.vladmihalcea.book.hpjp.util.AbstractTest;
import org.apache.commons.lang3.SerializationUtils;
import org.junit.Test;

import jakarta.persistence.*;
import java.io.Serializable;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import static org.junit.Assert.assertEquals;

/**
 * @author Vlad Mihalcea
 */
public class FluentSettersTest extends AbstractTest {

    @Override
    protected Class<?>[] entities() {
        return new Class<?>[] {
            Post.class,
            PostComment.class
        };
    }

    @Test
    public void testLifecycle() {
        doInJPA(entityManager -> {
            Post post = new Post()
            .setId(1L)
            .setTitle("High-Performance Java Persistence")
            .addComment(new PostComment().setReview("Awesome book")
                .setCreatedOn(Timestamp.from(
                    LocalDateTime.now().minusDays(1).toInstant(ZoneOffset.UTC))
                )
            )
            .addComment(new PostComment().setReview("High-Performance Rocks!")
                .setCreatedOn(Timestamp.from(
                    LocalDateTime.now().minusDays(2).toInstant(ZoneOffset.UTC))
                )
            )
            .addComment(new PostComment().setReview("Database essentials to the rescue!")
                .setCreatedOn(Timestamp.from(
                    LocalDateTime.now().minusDays(3).toInstant(ZoneOffset.UTC))
                )
            );
            entityManager.persist(post);

            Post clone = SerializationUtils.clone(post);
            assertEquals(post.getId(), clone.getId());
            assertEquals(post.getTitle(), clone.getTitle());
            assertEquals(post.getComments().size(), clone.getComments().size());
            assertEquals(post.getComments().get(0).getId(), clone.getComments().get(0).getId());
            assertEquals(post.getComments().get(0).getCreatedOn(), clone.getComments().get(0).getCreatedOn());
            assertEquals(post.getComments().get(0).getReview(), clone.getComments().get(0).getReview());
        });
        doInJPA(entityManager -> {
            Post post = entityManager.find(Post.class, 1L);
            assertEquals(3, post.getComments().size());
        });
    }

    @Entity(name = "Post")
    @Table(name = "post")
    public static class Post implements Serializable {

        @Id
        private Long id;

        private String title;

        public Post() {}

        public Post(String title) {
            this.title = title;
        }

        @OneToMany(cascade = CascadeType.ALL, orphanRemoval = true, mappedBy = "post")
        private List<PostComment> comments = new ArrayList<>();

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

        public List<PostComment> getComments() {
            return comments;
        }

        public Post addComment(PostComment comment) {
            comment.setPost(this);
            comments.add(comment);
            return this;
        }
    }

    @Entity(name = "PostComment")
    @Table(name = "post_comment")
    public static class PostComment implements Serializable {

        @Id
        @GeneratedValue
        private Long id;

        private String review;

        private Date createdOn;

        @ManyToOne
        private Post post;

        public Long getId() {
            return id;
        }

        public PostComment setId(Long id) {
            this.id = id;
            return this;
        }

        public String getReview() {
            return review;
        }

        public PostComment setReview(String review) {
            this.review = review;
            return this;
        }

        public Date getCreatedOn() {
            return createdOn;
        }

        public PostComment setCreatedOn(Date createdOn) {
            this.createdOn = createdOn;
            return this;
        }

        public Post getPost() {
            return post;
        }

        public PostComment setPost(Post post) {
            this.post = post;
            return this;
        }
    }
}
