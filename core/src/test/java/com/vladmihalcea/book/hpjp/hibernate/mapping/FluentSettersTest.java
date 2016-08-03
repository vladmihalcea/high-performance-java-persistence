package com.vladmihalcea.book.hpjp.hibernate.mapping;

import com.vladmihalcea.book.hpjp.util.AbstractTest;
import org.junit.Test;

import javax.persistence.*;
import java.util.ArrayList;
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
                .addComment(new PostComment("Awesome book"))
                .addComment(new PostComment("High-Performance Rocks!"))
                .addComment(new PostComment("Database essentials to the rescue!"));

            entityManager.persist(post);
        });
        doInJPA(entityManager -> {
            Post post = entityManager.find(Post.class, 1L);
            assertEquals(3, post.getComments().size());
        });
    }

    @Entity(name = "Post")
    @Table(name = "post")
    public static class Post {

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
    public static class PostComment {

        @Id
        @GeneratedValue
        private Long id;

        private String review;

        @ManyToOne
        private Post post;

        public PostComment() {}

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

        public Post getPost() {
            return post;
        }

        public void setPost(Post post) {
            this.post = post;
        }
    }
}
