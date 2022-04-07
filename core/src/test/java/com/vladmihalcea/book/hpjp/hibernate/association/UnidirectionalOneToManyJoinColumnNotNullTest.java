package com.vladmihalcea.book.hpjp.hibernate.association;

import com.vladmihalcea.book.hpjp.util.AbstractTest;
import org.junit.Test;

import jakarta.persistence.*;
import java.util.ArrayList;
import java.util.List;

/**
 * @author Vlad Mihalcea
 */
public class UnidirectionalOneToManyJoinColumnNotNullTest extends AbstractTest {

    @Override
    protected Class<?>[] entities() {
        return new Class<?>[] {
            Post.class,
            PostComment.class,
        };
    }

    @Test
    public void testRemoveTail() {
        doInJPA(entityManager -> {
            Post post = new Post("First post");

            post.getComments().add(new PostComment("My first review"));
            post.getComments().add(new PostComment("My second review"));
            post.getComments().add(new PostComment("My third review"));

            entityManager.persist(post);
            entityManager.flush();

            LOGGER.info("Remove tail");
            post.getComments().remove(2);
        });
    }

    @Test
    public void testRemoveHead() {
        doInJPA(entityManager -> {
            Post post = new Post("First post");

            post.getComments().add(new PostComment("My first review"));
            post.getComments().add(new PostComment("My second review"));
            post.getComments().add(new PostComment("My third review"));

            entityManager.persist(post);
            entityManager.flush();

            entityManager.flush();
            LOGGER.info("Remove head");
            post.getComments().remove(0);
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

        @OneToMany(cascade = CascadeType.ALL, orphanRemoval = true)
        @JoinColumn(name = "post_id", nullable = false)
        private List<PostComment> comments = new ArrayList<>();

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
    }

    @Entity(name = "PostComment")
    @Table(name = "post_comment")
    public static class PostComment {

        @Id
        @GeneratedValue
        private Long id;

        private String review;

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
    }
}
