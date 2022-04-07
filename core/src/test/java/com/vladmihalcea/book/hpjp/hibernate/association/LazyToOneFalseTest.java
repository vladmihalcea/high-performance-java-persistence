package com.vladmihalcea.book.hpjp.hibernate.association;

import com.vladmihalcea.book.hpjp.util.AbstractTest;
import org.hibernate.annotations.LazyToOne;
import org.hibernate.annotations.LazyToOneOption;
import org.junit.Test;

import jakarta.persistence.*;
import java.util.Date;

import static org.junit.Assert.assertNotNull;

/**
 * @author Vlad Mihalcea
 */
public class LazyToOneFalseTest extends AbstractTest {

    @Override
    protected Class<?>[] entities() {
        return new Class<?>[]{
            Post.class,
            PostComment.class
        };
    }

    @Test
    public void testLazyLoadingNoProxy() {
        final Post post = new Post()
            .setId(1L)
            .setTitle("High-Performance Java Persistence, 1st Part");

        doInJPA(entityManager -> {
            entityManager.persist(post);

            entityManager.persist(
                new PostComment()
                    .setId(1L)
                    .setReview("Amazing!")
                    .setPost(post)
            );
        });

        PostComment comment = doInJPA(entityManager -> {
            return entityManager.find(PostComment.class, 1L);
        });

        assertNotNull(comment.getPost());
    }

    @Entity(name = "Post")
    @Table(name = "post")
    public static class Post {

        @Id
        private Long id;

        private String title;

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
    }

    @Entity(name = "PostComment")
    @Table(name = "post_comment")
    public static class PostComment {

        @Id
        private Long id;

        private String review;

        @ManyToOne(fetch = FetchType.LAZY)
        //@LazyToOne(LazyToOneOption.FALSE)
        @JoinColumn(name = "post_id")
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

        public Post getPost() {
            return post;
        }

        public PostComment setPost(Post post) {
            this.post = post;
            return this;
        }
    }
}
