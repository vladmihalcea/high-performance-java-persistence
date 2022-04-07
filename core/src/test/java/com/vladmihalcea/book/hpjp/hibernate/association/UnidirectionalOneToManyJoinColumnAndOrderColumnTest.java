package com.vladmihalcea.book.hpjp.hibernate.association;

import java.util.ArrayList;
import java.util.List;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderColumn;
import jakarta.persistence.Table;

import org.junit.Test;

import com.vladmihalcea.book.hpjp.util.AbstractTest;

/**
 * @author Vlad Mihalcea
 */
public class UnidirectionalOneToManyJoinColumnAndOrderColumnTest extends AbstractTest {

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
        @JoinColumn(name = "post_id")
        @OrderColumn(name = "order_id")
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
