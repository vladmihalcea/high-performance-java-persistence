package com.vladmihalcea.book.hpjp.hibernate.association;

import com.vladmihalcea.book.hpjp.util.AbstractTest;
import org.junit.Test;

import javax.persistence.*;

/**
 * <code>ManyToOneTest</code> - @ManyToOne Test
 *
 * @author Vlad Mihalcea
 */
public class ManyToOneTest extends AbstractTest {

    @Override
    protected Class<?>[] entities() {
        return new Class<?>[] {
            Post.class,
            PostComment.class,
        };
    }

    @Test
    public void testLifecycle() {
        doInJPA(entityManager -> {
            Post post = new Post("First post");
            entityManager.persist(post);
        });
        doInJPA(entityManager -> {
            Post post = entityManager.find(Post.class, 1L);
            PostComment comment = new PostComment("My review");
            comment.setPost(post);
            entityManager.persist(comment);

            entityManager.flush();
            comment.setPost(null);
        });
    }

    @Entity(name = "Post")
    @Table(name = "post")
    public static class Post {

        @Id
        @GeneratedValue
        private Long id;

        private String title;

        public Post() {}

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
    }

    @Entity(name = "PostComment")
    @Table(name = "post_comment")
    public static class PostComment {

        @Id
        @GeneratedValue
        private Long id;

        private String review;

        @ManyToOne
        @JoinColumn(name = "post_id")
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
