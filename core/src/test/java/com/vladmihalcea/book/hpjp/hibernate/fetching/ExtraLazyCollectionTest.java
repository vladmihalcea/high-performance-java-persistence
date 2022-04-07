package com.vladmihalcea.book.hpjp.hibernate.fetching;

import com.vladmihalcea.book.hpjp.util.AbstractTest;
import org.hibernate.annotations.Fetch;
import org.hibernate.annotations.FetchMode;
import org.hibernate.annotations.LazyCollection;
import org.hibernate.annotations.LazyCollectionOption;
import org.junit.Test;

import jakarta.persistence.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import static org.junit.Assert.assertEquals;

/**
 * @author Vlad Mihalcea
 */
public class ExtraLazyCollectionTest extends AbstractTest {

    @Override
    protected Class<?>[] entities() {
        return new Class<?>[]{
            Post.class,
            PostComment.class
        };
    }

    @Test
    public void test() {
        doInJPA(entityManager -> {
            entityManager.persist(
                new Post()
                .setId(1L)
                .setTitle("High-Performance Java Persistence")
                .addComment(
                    new PostComment()
                    .setId(1L)
                    .setReview("Excellent book to understand Java persistence")
                )
                .addComment(
                    new PostComment()
                    .setId(2L)
                    .setReview("The best JPA ORM book out there")
                )
                .addComment(
                    new PostComment()
                    .setId(3L)
                    .setReview("Must-read for Java developers")
                )
            );
        });

        LOGGER.info("Fetch comments with for-each loop");

        doInJPA(entityManager -> {
            Post post = entityManager.find(Post.class, 1L);

            for (PostComment comment: post.getComments()) {
                LOGGER.info("{} book review: {}",
                    post.getTitle(),
                    comment.getReview()
                );
            }
        });

        LOGGER.info("Fetch comments with for loop");

        doInJPA(entityManager -> {
            Post post = entityManager.find(Post.class, 1L);

            int commentCount = post.getComments().size();

            for(int i = 0; i < commentCount; i++ ) {
                PostComment comment = post.getComments().get(i);
                LOGGER.info("{} book review: {}",
                    post.getTitle(),
                    comment.getReview()
                );
            }
        });
    }

    @Entity(name = "Post")
    @Table(name = "post")
    public static class Post {

        @Id
        private Long id;

        private String title;

        @OneToMany(mappedBy = "post", cascade = CascadeType.ALL, orphanRemoval = true)
        @LazyCollection(LazyCollectionOption.EXTRA)
        @OrderColumn(name = "order_id")
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
            comments.add(comment);
            comment.setPost(this);
            return this;
        }

        public Post removeComment(PostComment comment) {
            comments.remove(comment);
            comment.setPost(null);
            return this;
        }
    }

    @Entity(name = "PostComment")
    @Table(name = "post_comment")
    public static class PostComment {

        @Id
        private Long id;

        @ManyToOne(fetch = FetchType.LAZY)
        private Post post;

        private String review;

        public Long getId() {
            return id;
        }

        public PostComment setId(Long id) {
            this.id = id;
            return this;
        }

        public Post getPost() {
            return post;
        }

        public PostComment setPost(Post post) {
            this.post = post;
            return this;
        }

        public String getReview() {
            return review;
        }

        public PostComment setReview(String review) {
            this.review = review;
            return this;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            return id != null && id.equals(((PostComment) o).getId());
        }

        @Override
        public int hashCode() {
            return getClass().hashCode();
        }
    }
}
